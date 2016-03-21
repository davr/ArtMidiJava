package org.davr.ArtMidiJava;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.SysexMessage;
import javax.sound.midi.Transmitter;

import net.dhleong.acl.enums.ConnectionType;
import net.dhleong.acl.enums.ObjectType;
import net.dhleong.acl.enums.ShipSystem;
import net.dhleong.acl.iface.ArtemisNetworkInterface;
import net.dhleong.acl.iface.DisconnectEvent;
import net.dhleong.acl.iface.Listener;
import net.dhleong.acl.iface.ListenerRegistry;
import net.dhleong.acl.iface.PacketFactory;
import net.dhleong.acl.iface.PacketFactoryRegistry;
import net.dhleong.acl.iface.PacketReader;
import net.dhleong.acl.iface.ThreadedArtemisNetworkInterface;
import net.dhleong.acl.protocol.ArtemisPacket;
import net.dhleong.acl.protocol.RawPacket;
import net.dhleong.acl.protocol.core.eng.EngSetCoolantPacket;
import net.dhleong.acl.protocol.core.eng.EngSetEnergyPacket;
import net.dhleong.acl.protocol.core.world.ObjectUpdatePacket;
import net.dhleong.acl.util.ByteArrayReader;
import net.dhleong.acl.world.ArtemisPlayer;
import net.dhleong.acl.world.SystemManager;

import	javax.sound.midi.Receiver;


public class ArtemisProxy implements Runnable {
    public static void main(String[] args) {
    	if(args.length < 1) {
    		System.out.println("Usage: ArtMidiJava remoteserver[:port] [localport]\n remoteserver - address (and optionally port) of the remote Artemis Server\n localport - local port to listen on for connections from artemis client\n" );
    		System.exit(1);
    	}
        String serverAddr = args[0];
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 2010;
        new Thread(new ArtemisProxy(port, serverAddr)).start();
    }

    private int port;
    private String serverAddr;
    private int serverPort = 2010;
//    private PacketPersistingProxyDebugger debugger;
    public ThreadedArtemisNetworkInterface client;
    public ThreadedArtemisNetworkInterface server;
    
    public MidiDevice midiDevice = null;
    public Receiver midi = null;
    
    public ArtemisProxy(int port, String serverAddr) {
        this.port = port;
        int colonPos = serverAddr.indexOf(':');

        if (colonPos == -1) {
            this.serverAddr = serverAddr;
        } else {
            this.serverAddr = serverAddr.substring(0, colonPos);
            serverPort = Integer.parseInt(serverAddr.substring(colonPos + 1));
        }
    }

    @Override
    public void run() {
        ServerSocket listener = null;

        try {
            MidiCommon.listDevicesAndExit(true, true, true);
        	
            String strDeviceName = "LoopBe Internal MIDI";
    		MidiDevice.Info info;
    		//info = MidiCommon.getMidiDeviceInfo(strDeviceName, false);
    		info = MidiCommon.getMidiDeviceInfo(1);
    		if (info == null) {
    			out("no device info found for name " + strDeviceName);
    		} else {
    			out(info.toString());
    		}
    		try {
    			midiDevice = MidiSystem.getMidiDevice(info);
    			midiDevice.open();
    			out("opened midi");
    		} catch (Exception e) {
    			out("Error opening midi device:");
    			out(e.toString());
    		}
    		if (midiDevice == null) {
    			out("wasn't able to retrieve MidiDevice");
    			//System.exit(1);
    		}
    		
    		try {
    			midi = midiDevice.getReceiver();
    			out("connected output to midi");
    		} catch (Exception e) {
    			
    			out("First output attempt failed:");
    			out(e.toString());
    			try {
					MidiDevice device2 = MidiSystem.getMidiDevice(MidiCommon.getMidiDeviceInfo(4));
					midi = device2.getReceiver();
					out("connected output to midi (try 2)");
				} catch (Exception e1) {
	    			out("wasn't able to get output to midi device:");
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
    		}

            listener = new ServerSocket(this.port, 0);
            listener.setSoTimeout(0);
            System.out.println("Listening for connections on port " + this.port + "...");
            Socket skt = listener.accept();
            System.out.println("Received connection from " + skt.getRemoteSocketAddress());
            client = new ThreadedArtemisNetworkInterface(skt, ConnectionType.CLIENT);
            client.setParsePackets(false);
            System.out.println("Connecting to server at " + serverAddr + ":" + serverPort + "...");
            server = new ThreadedArtemisNetworkInterface(serverAddr, serverPort);
            server.setParsePackets(false);
            ProxyListener prox = new ProxyListener(server, client, server.factoryRegistry);
            System.out.println("Connection established.");
    		    		
    		try {
    			Transmitter t = midiDevice.getTransmitter();
    			t.setReceiver(prox);
    			out("connected input from midi");
    		} catch (MidiUnavailableException e) {
    			out("wasn't able to connect input from midi device to our proxy");
    			out(e.toString());
    			//midiDevice.close();
    			//System.exit(1);
    		}
            
            
            
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            if (listener != null && !listener.isClosed()) {
                try {
                    listener.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    public class ProxyListener implements Receiver {
        private ArtemisNetworkInterface server;
        private ArtemisNetworkInterface client;
        private PacketFactoryRegistry factoryRegistry;
        public SystemManager world;

        private ProxyListener(ArtemisNetworkInterface server, ArtemisNetworkInterface client, PacketFactoryRegistry factoryRegistry) {
            this.server = server;
            this.client = client;
            this.factoryRegistry = factoryRegistry;
            world = new SystemManager();
            server.addListener(this);
            client.addListener(this);
            server.start();
            client.start();
        }

        @Listener
        public void onDisconnect(DisconnectEvent event) {
            server.stop();
            client.stop();
            System.out.println("Disconnect: " + event);

            if (event.getException() != null) {
                event.getException().printStackTrace();
            }
        }
/*
        # Controls:
        	# 0x00 - 0x07: sliders
        	# 0x10 - 0x17: knobs
        	# 0x20 - 0x27: S buttons
        	# 0x30 - 0x37: M buttons
        	# 0x40 - 0x47: R buttons
*/
        public void setLight(int btn, boolean on) {
        	if(midi == null) return;
        	try {
				midi.send(new ShortMessage(176, btn, on?127:0), -1);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				out("Error setting lights:");
				e.printStackTrace();
			}
        }

        @Listener
        public void onPacket(RawPacket pkt) {
            ConnectionType type = pkt.getConnectionType();
            ArtemisNetworkInterface dest = type == ConnectionType.SERVER ? client : server;
            dest.send(pkt);
            
            if(type != ConnectionType.SERVER) return;
            
    		// Find the PacketFactory that knows how to handle this packet type
    		PacketFactory factory = null;
    		byte subtype = pkt.getPayload().length > 0 ? pkt.getPayload()[0] : 0x00;

   			factory = factoryRegistry.get(type, pkt.getType(), subtype);
   			
   			if(factory == null)
   				return;
   			
   			if(pkt.getType() == ObjectUpdatePacket.TYPE) {
   				InputStream in = new ByteArrayInputStream(pkt.getPayload());
				ListenerRegistry listenerRegistry = new ListenerRegistry();
				PacketReader reader = new PacketReader(type, in, factoryRegistry, listenerRegistry);
				reader.payload = new ByteArrayReader(pkt.getPayload());
				ObjectUpdatePacket oPkt = new ObjectUpdatePacket(reader);

	            
	            world.onPacket(oPkt);
	            
	            if(oPkt.objectType == ObjectType.ENGINEERING_CONSOLE) {
		            //System.out.println(type + "> " + oPkt);
	            }
	            
				
   			}
   			
   			if(System.currentTimeMillis() - lastUpdateHeat >= 250)
   				updateHeat();
            
        }
        
        private long lastUpdateHeat;
        
        
        // --- Midi Stuff ---

    	public void close()
    	{
    	}

    	public void send(MidiMessage message, long lTimeStamp)
    	{
    		String	strMessage = null;
    		if (message instanceof ShortMessage)
    		{
    			strMessage = decodeMessage((ShortMessage) message);
    		}
    		else
    		{
    			strMessage = "unknown message type";
    		}
    		String	strTimeStamp = null;
    		
			if (lTimeStamp == -1L)
			{
				strTimeStamp = "timestamp [unknown]: ";
			}
			else
			{
				strTimeStamp = "timestamp " + lTimeStamp + " us: ";
			}
			
    		System.out.println(strTimeStamp + strMessage);
    	}
    	

    	/*
        # Controls:
        	# 0x00 - 0x07: sliders
        	# 0x10 - 0x17: knobs
        	# 0x20 - 0x27: S buttons
        	# 0x30 - 0x37: M buttons
        	# 0x40 - 0x47: R buttons
*/
    	public void setEnergy(int control, double energy) {
    		out(ShipSystem.values()[control]+" to "+energy);
			EngSetEnergyPacket ePkt = new EngSetEnergyPacket(ShipSystem.values()[control], (float)energy);
    		server.send(ePkt);    		    		
    	}
    	
    	public void setCoolant(int control, int coolant) {
    		out(ShipSystem.values()[control]+" cool "+coolant);
			EngSetCoolantPacket cPkt = new EngSetCoolantPacket(ShipSystem.values()[control], coolant);
			server.send(cPkt);    		
    	}
    	
    	public int[] coolants = new int[8];
    	
    	public void updateCoolants() {
    		int sum=0;
    		for(int i=0; i<8; i++)
    			sum += coolants[i];    		
    		
    	}
    	
    	public void setLights(int control, boolean S, boolean M, boolean R) {
    		setLight(0x20 + control, S);
    		setLight(0x30 + control, M);
    		setLight(0x40 + control, R);
    	}
    	
    	double[] heats = new double[8];
    	
    	public void updateHeat() {
    		if(world == null) return;
            ArtemisPlayer player = world.getPlayerShip(0);            
    		if(player == null) return;
            for(int i=0; i<8; i++) {
            	double heat = player.getSystemHeat(ShipSystem.values()[i]);
            	if(heat < 0.10) {
            		setLights(i, false, false, false);
            	} else if(heat < 0.30) {
            		setLights(i, true, false, false);
	            } else if(heat < 0.50) {
	        		setLights(i, true, true, false);
	            } else if(heat < 0.70) {
	        		setLights(i, true, true, true);
	            } else {
	            	if(System.currentTimeMillis() % 500 < 250)
	            		setLights(i, true, true, true);
	            	else
	            		setLights(i, false, false, false);
	        	}
            }
            	
    	}
    	
    	public String decodeMessage(ShortMessage message)
    	{
    		String	strMessage = null;
    		switch (message.getCommand())
    		{
    		case 0xb0:
    			strMessage = "control change " + message.getData1() + " value: " + message.getData2();
    			int control = message.getData1();
    			int value = message.getData2();
    			
    			// sliders
    			if(control >= 0x00 && control <= 0x07) {
    				double energy = value/127.0;
    				
    				setEnergy(control, energy);
    			}
    			
    			if(control >= 0x10 && control <= 0x17) {
    				coolants[control - 0x10] = value;
    				updateCoolants();
    				
    				setCoolant(control - 0x10, (value * 8)/127);
    			}
    			break;

    		default:
    			strMessage = "unknown message: "+message.getCommand()+" status = " + message.getStatus() + ", byte1 = " + message.getData1() + ", byte2 = " + message.getData2();
    			break;
    		}
    		if (message.getCommand() != 0xF0)
    		{
    			int	nChannel = message.getChannel() + 1;
    			String	strChannel = "channel " + nChannel + ": ";
    			strMessage = strChannel + strMessage;
    		}
    	//	smCount++;
    	//	smByteCount+=message.getLength();
    		return /*"["+getHexString(message)+"] "+*/strMessage;
    	}


    }
    
	private static void out(String strMessage) {
		System.out.println(strMessage);
	}    
}