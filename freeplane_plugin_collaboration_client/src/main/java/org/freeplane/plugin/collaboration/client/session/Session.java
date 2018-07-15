package org.freeplane.plugin.collaboration.client.session;

import java.awt.EventQueue;
import java.util.List;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.atomic.AtomicReference;

import org.freeplane.collaboration.event.MapUpdated;
import org.freeplane.collaboration.event.messages.Credentials;
import org.freeplane.collaboration.event.messages.ImmutableMapUpdateRequested;
import org.freeplane.collaboration.event.messages.MapId;
import org.freeplane.collaboration.event.messages.MapUpdateProcessed.UpdateStatus;
import org.freeplane.collaboration.event.messages.UpdateBlockCompleted;
import org.freeplane.core.extension.IExtension;
import org.freeplane.features.map.mindmapmode.MMapModel;
import org.freeplane.plugin.collaboration.client.event.UpdateProcessorChain;
import org.freeplane.plugin.collaboration.client.event.batch.Updates;
import org.freeplane.plugin.collaboration.client.server.ImmutableSubscription;
import org.freeplane.plugin.collaboration.client.server.Server;
import org.freeplane.plugin.collaboration.client.server.Subscription;

public class Session implements IExtension{

	private static final int DELAY = 100;
	private final MapId mapId;
	private final MMapModel map;
	private final Subscription subscription;
	private final Server server;
	private final SessionUndoHandler undoHandler;
	private final Updates updates;
	private final Credentials credentials;
	private final UpdateProcessorChain updateProcessor;
	private final LinkedTransferQueue<UpdateBlockCompleted> incomingEvents;
	private AtomicReference<UpdateStatus> updateStatus;
	private boolean isRunning;
	private long mapRevision;
	public MapId getMapId() {
		return mapId;
	}

	public Session(Server server, Credentials credentials, UpdateProcessorChain updateProcessor, MapId mapId, MMapModel map) {
		this.server = server;
		this.credentials = credentials;
		this.updateProcessor = updateProcessor;
		this.mapId = mapId;
		this.map = map;
		updateStatus = new AtomicReference<>(UpdateStatus.ACCEPTED);
		map.addExtension(this);
		undoHandler = new SessionUndoHandler(map);
		updates = new Updates(this::sendUpdate, DELAY);
		mapRevision = 0;
		map.addExtension(this);
		map.addExtension(updates);
		subscription = ImmutableSubscription.builder().credentials(credentials).mapId(mapId).consumer(this::consumeUpdate).build();
		server.subscribe(subscription);
		incomingEvents = new LinkedTransferQueue<>();
		isRunning = true;

	}

	void consumeUpdate(UpdateBlockCompleted ev) {
		if (isRunning && (! ev.userId().equals(credentials.userId()) || updateStatus.get() == UpdateStatus.MERGED)) {
			incomingEvents.add(ev);
			EventQueue.invokeLater(new Runnable() {
				@Override
				public void run() {
					applyUpdate();
				}
			});
}
	}
	private void applyUpdate() {
		if(undoHandler.getTransactionLevel() > 0 && updateStatus.get() != UpdateStatus.MERGED)
			return;
		UpdateBlockCompleted incomingEvent= incomingEvents.poll();
		if(incomingEvent == null)
			return;
		map.removeExtension(Updates.class);
		try {
			updateProcessor.onUpdate(map, incomingEvent.updateBlock());
			mapRevision = incomingEvent.mapRevision();
			if(undoHandler.getTransactionLevel() == 1) {
				undoHandler.commit();
			}
		}
		finally {
			map.addExtension(Updates.class, updates);
		}
	}

	void sendUpdate(List<MapUpdated> events) {
		if(! incomingEvents.isEmpty()) {
			mergeUpdates(events);
		}
		UpdateBlockCompleted event = UpdateBlockCompleted.builder()
				.userId(credentials.userId())
				.mapId(mapId)
				.updateBlock(events).mapRevision(mapRevision + 1).build();
		this.updateStatus.set(UpdateStatus.MERGED);
		this.server.update(ImmutableMapUpdateRequested.of(this.credentials, event))
		.thenAccept(updateStatus -> EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {
				switch(updateStatus) {
					case ACCEPTED:
						mapRevision++;
							undoHandler.commit();
							Session.this.updateStatus.set(updateStatus);
						break;
					case REJECTED:{
						rollback();
					}
					Session.this.updateStatus.set(UpdateStatus.ACCEPTED);
						break;
					case MERGED:
						Session.this.updateStatus.set(UpdateStatus.MERGED);
						break;
				}
			}
		}));
	}

	private void rollback() {
		this.map.removeExtension(Updates.class);
		try {
			undoHandler.rollback();
		}
		finally {
			this.map.addExtension(Updates.class, updates);
		}
	}

	private void mergeUpdates(List<MapUpdated>  ev) {
		final Updates extension = map.removeExtension(Updates.class);
		try {
			undoHandler.rollback();
			UpdateBlockCompleted incomingEvent;
			while( ! incomingEvents.isEmpty()){
				incomingEvent = incomingEvents.poll();
				updateProcessor.onUpdate(map, incomingEvent.updateBlock());
				mapRevision = incomingEvent.mapRevision();
				undoHandler.commit();
			}
			updateProcessor.onUpdate(map, ev);
		}
		finally {
			map.addExtension(Updates.class, extension);
		}
	}

	void terminate() {
		map.removeExtension(Session.class);
		map.removeExtension(Updates.class);
		undoHandler.removeFromMap();
		server.unsubscribe(subscription);
	}
}