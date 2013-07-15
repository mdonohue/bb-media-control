package com.whatsapp.audiotest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;
import javax.microedition.media.MediaException;
import javax.microedition.media.Player;

import net.rim.device.api.ui.Field;
import net.rim.device.api.ui.component.ButtonField;
import net.rim.device.api.ui.component.Dialog;
import net.rim.device.api.ui.component.ObjectChoiceField;
import net.rim.device.api.ui.container.MainScreen;

/**
 * A class extending the MainScreen class, which provides default standard
 * behavior for BlackBerry GUI applications.
 */
public final class MyScreen extends MainScreen {
	private int PLAYER_COUNT = 6;
	private Player[] _players = new Player[PLAYER_COUNT];

	public MyScreen() {
		setTitle("WhatsApp Audio Test");

		Object[] choices = new Object[PLAYER_COUNT];
		for (int i = 0; i < PLAYER_COUNT; i++) {
			choices[i] = Integer.toString(i + 1);
		}

		final ObjectChoiceField playerIds = new ObjectChoiceField("Player", choices);
		playerIds.setMargin(6, 12, 6, 12);

		final ObjectChoiceField extensions = new ObjectChoiceField("Extension", new Object[] { "amr", "aac", "mp3" }, 0);
		extensions.setMargin(6, 12, 6, 12);

		final String directFile = "DirectFile";
		final String inputStream = "InputStream";

		final ObjectChoiceField loadType = new ObjectChoiceField("Load Type", new Object[] { directFile, inputStream },
				0);
		loadType.setMargin(6, 12, 6, 12);

		final ObjectChoiceField targetState = new ObjectChoiceField("Target State", new Object[] { "Realize",
				"Prefetch", "Start" });
		targetState.setMargin(6, 12, 6, 12);

		ButtonField button = new ButtonField("Go", Field.FIELD_RIGHT | ButtonField.CONSUME_CLICK);
		button.setMargin(24, 12, 24, 12);

		button.setRunnable(new Runnable() {
			public void run() {
				Thread t = new Thread(new Runnable() {
					public void run() {
						try {
							int idx = targetState.getSelectedIndex();
							String playerTarget = (String) targetState.getChoice(idx);

							Player p = getPlayer();
							if (playerTarget.equals("Realize")) {
								p.realize();
							} else if (playerTarget.equals("Prefetch")) {
								p.realize();
								p.prefetch();
							} else if (playerTarget.equals("Start")) {
								p.realize();
								p.prefetch();
								p.start();
							}
						} catch (final Throwable t) {
							MyApp.getApplication().invokeLater(new Runnable() {
								public void run() {
									Dialog.inform("Caught throwable in 'Go': " + t.getClass().getName());
								}
							});
						}
					}
				});
				t.start();
			}

			private Player getPlayer() throws MediaException, IOException {
				int extensionIdx = extensions.getSelectedIndex();
				String extChoice = (String) extensions.getChoice(extensionIdx);

				String filename = "WhatsApp-Test." + extChoice;
				String loadTypeStr = (String) loadType.getChoice(loadType.getSelectedIndex());

				Player p;
				if (loadTypeStr == inputStream) {
					String mimeType = "audio/" + extChoice;
					p = createPlayerByStream(filename, mimeType);
				} else if (loadTypeStr == directFile) {
					p = createPlayerByFile(filename);
				} else {
					MyApp.getApplication().invokeLater(new Runnable() {
						public void run() {
							Dialog.inform("Unknown load type");
						}
					});
					return null;
				}

				int playerSelectedIdx = playerIds.getSelectedIndex();
				String playerIdxString = (String) playerIds.getChoice(playerSelectedIdx);
				int playerIdx = Integer.parseInt(playerIdxString) - 1;

				Player fromList = _players[playerIdx];
				if (fromList != null) {
					fromList.stop();
					fromList.deallocate();
					_players[playerIdx] = null;
				}
				_players[playerIdx] = p;

				return p;
			}
		});

		this.add(playerIds);
		this.add(extensions);
		this.add(loadType);
		this.add(targetState);
		this.add(button);
	}

	public static Player createPlayerByFile(String filename) throws MediaException, IOException {
		final String mediaRoot = "file:///store/home/user/voicenotes/";
		String fullFilename = mediaRoot + filename;
		FileConnection fConn = (FileConnection) Connector.open(fullFilename);
		if (!fConn.exists()) {
			fConn.create();
		}
		if (fConn.fileSize() == 0) {
			OutputStream os = fConn.openOutputStream();
			InputStream is = MyScreen.class.getResourceAsStream("/audio/" + filename);
			byte[] buf = new byte[8192];
			int readSize = 0;
			while ((readSize = is.read(buf, 0, buf.length)) != -1) {
				os.write(buf, 0, readSize);
			}
			os.close();
			is.close();
		}
		fConn.close();
		return javax.microedition.media.Manager.createPlayer(fullFilename);
	}

	public static Player createPlayerByStream(String filename, String mimeType) throws MediaException, IOException {
		InputStream inputStream = MyScreen.class.getResourceAsStream("/audio/" + filename);
		return javax.microedition.media.Manager.createPlayer(inputStream, mimeType);
	}
}
