package ua.p2psafety.services;

import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.util.Xml;

import org.jivesoftware.smack.AndroidConnectionConfiguration;
import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.SmackAndroid;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.MessageTypeFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smackx.pubsub.ItemPublishEvent;
import org.jivesoftware.smackx.pubsub.LeafNode;
import org.jivesoftware.smackx.pubsub.Node;
import org.jivesoftware.smackx.pubsub.PubSubManager;
import org.jivesoftware.smackx.pubsub.Subscription;
import org.jivesoftware.smackx.pubsub.listener.ItemEventListener;
import org.xmlpull.v1.XmlPullParser;

import java.io.File;
import java.io.StringReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ua.p2psafety.SosActivity;
import ua.p2psafety.data.Prefs;
import ua.p2psafety.fragments.AcceptEventFragment;
import ua.p2psafety.json.VictimData;
import ua.p2psafety.util.EventManager;
import ua.p2psafety.util.Logs;
import ua.p2psafety.util.Utils;

public class XmppService extends Service {
    private static final String TAG = "XmppService";
    private static final String HOST = "p2psafety.net";

    public static final String SUPPORTER_URL_KEY = "SUPPORTER_URL";
    public static final String LOCATION_KEY = "LOCATION";
    public static final String LAST_COMMENT_KEY = "LAST_COMMENT";
    public static final String VICTIM_NAME_KEY = "VICTIM_NAME";

    // while asking user to accept some event,
    // don't ask him about other events
    public static boolean processing_event = false;

    XMPPConnection mConnection;
    PacketListener mPacketListener;
    ItemEventListener mItemEventListener;
    LeafNode mNode;

    String mUserLogin;
    String mUserPassword;
    String mUserJid;

    // parsed data from xmpp-message
    public static VictimData VICTIM_DATA = new VictimData();

    Logs logs;
    SmackAndroid mSmackAndroid;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        logs = new Logs(this);
        logs.info("XmppService started");

        connectToServer();
        return Service.START_STICKY;
    }

    private void connectToServer() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(new Runnable() {
            @Override
            public void run() {
                logs.info("Getting xmpp connection configuration");
                mSmackAndroid = SmackAndroid.init(XmppService.this);
                mConnection = getConfiguredConnection(HOST);

                try {
                    mUserLogin = Prefs.getApiUsername(XmppService.this);
                    mUserPassword = Prefs.getApiKey(XmppService.this);
                    mUserJid = mUserLogin + "@" + HOST;

                    logs.info("Connecting to xmpp server");
                    Log.i("XmppService", "login: " + mUserLogin + " password: " + mUserPassword +
                            " jid: " + mUserJid);

                    mConnection.connect();
                    mConnection.login(mUserLogin, mUserPassword);
                } catch (Exception e) {
                    Log.i(TAG, "Error during connection");
                    e.printStackTrace();
                    return;
                }

                setMessageListener(mConnection);
                setPubsubListener(mConnection);
            }
        });
    }

    private XMPPConnection getConfiguredConnection(String host) {
        XMPPConnection connection;
        try {
            AndroidConnectionConfiguration connConfig = new AndroidConnectionConfiguration(host);

            // don't ask me what this code does, I don't know :)  (it is required though)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                connConfig.setTruststoreType("AndroidCAStore");
                connConfig.setTruststorePassword(null);
                connConfig.setTruststorePath(null);
            } else {
                connConfig.setTruststoreType("BKS");
                String path = System.getProperty("javax.net.ssl.trustStore");
                if (path == null)
                    path = System.getProperty("java.home") + File.separator + "etc"
                            + File.separator + "security" + File.separator
                            + "cacerts.bks";
                connConfig.setTruststorePath(path);
            }

            connection = new XMPPConnection(connConfig);
        } catch (Exception e) {
            connection = null;
        }

        return connection;
    }

    // this is the listener for normal (personal) messages
    private void setMessageListener(final Connection connection) {
        mPacketListener = new PacketListener() {
            public void processPacket(Packet packet) {
                try {
                    Message mes = (Message) packet;
                    Log.i("got personal message", "xml: " + mes.toXML());
                    logs.info("Got personal xmpp message: " + mes.toXML());

                    parseXml(mes.toXML());

                    // check if event is in acceptable distance and if so show it
                    Location location = LocationService.locationListener.getLastLocation(false);
                    Long radius = VICTIM_DATA.getRadius();
                    if (location == null || radius == 0 ||
                            location.distanceTo(VICTIM_DATA.getLocation()) <= radius)
                    {
                        if (!processing_event
                                && !EventManager.getInstance(XmppService.this).isEventActive())
                        {
                            openAcceptEventScreen();
                            processing_event = true;
                        }
                    }
                } catch (Exception e) {}
            }
        };
        connection.addPacketListener(mPacketListener,
                new MessageTypeFilter(Message.Type.chat));
    }

    // this is the listener for pubsub (global) messages
    private void setPubsubListener(Connection connection) {
        try {
            PubSubManager pbManager = new PubSubManager(connection);

            // TODO: delete after debug
//            DiscoverItems nodes = pbManager.discoverNodes(null);
//            Iterator<DiscoverItems.Item> node = nodes.getItems();
//            while (node.hasNext()) {
//                DiscoverItems.Item n = node.next();
//                Log.i("xmpp nodes", "Node name: " + n.getNode());
//            }

            logs.info("getting xmpp pubsub node");
            mNode = pbManager.getNode("events");
            mNode.addItemEventListener(new ItemEventListener() {
                @Override
                public void handlePublishedItems(ItemPublishEvent items) {
                    EventManager eventManager = EventManager.getInstance(XmppService.this);
                    if (items.isDelayed() || eventManager.isSupportStarted()
                            || eventManager.isSosStarted() || eventManager.isPassiveSosStarted())
                    {
                        return; // old event or support started or sos started
                    }

                    startService(new Intent(XmppService.this, LocationService.class));
                    Log.i("got pubsub message", "Item count: " + items.getItems().size());
                    Log.i("===================", items.toString());
                    Log.i("===================", items.getItems().get(0).toString());
                    Log.i("===================", "===================================");
                    logs.info("got xmpp pubsub message");
                    try {
                        String message = mNode.getItems(1).get(0).toXML();
                        Log.i("===================", "xml: " + message);
                        parseXml(message);
                    } catch (XMPPException e) {}
                    Log.i("===================", "===================================");

                    // check if event is in acceptable distance and if so show it
                    Location location = LocationService.locationListener.getLastLocation(false);
                    Long radius = VICTIM_DATA.getRadius();
                    if (location == null || radius == 0 ||
                            location.distanceTo(VICTIM_DATA.getLocation()) <= radius)
                    {
                        Log.i("pubsub", "processing_event: " + String.valueOf(processing_event));
                        Log.i("pubsub", "is server authed: " + String.valueOf(Utils.isServerAuthenticated(XmppService.this)));
                        Log.i("pubsub", "isEventActive: " + String.valueOf(EventManager.getInstance(XmppService.this).isEventActive()));
                        if (!processing_event &&
                            Utils.isServerAuthenticated(XmppService.this) &&
                            !EventManager.getInstance(XmppService.this).isEventActive())
                        {
                            openAcceptEventScreen();
                            processing_event = true;
                        }
                    }
                }
            });

            if (!isSubscribed(mNode, mUserJid)) {
                Log.i(TAG, "making new subscription");
                logs.info("subscribing to xmpp pubsub node. Node name: " + mNode.getId());
                mNode.subscribe(mUserJid);
            }

            // TODO: delete after debug
//            List<Item> items = testNode.getItems();
//            for (Item item: items)
//                Log.i("getItems()", "xml: " + item.toXML());
//
//            testNode.send(new PayloadItem("Uvs"+"*" + System.currentTimeMillis(),
//                    new SimplePayload("...",
//                            "stage:pubsub:simple", "<book xmlns='pubsub:test:book'><title>Lord of the Rings</title></book>")));

        } catch (Exception e) {
            Log.i("xmpp pubsub", "Shit happened");
            logs.info("got error with xmpp pubsub");
            e.printStackTrace();
        }
    }

    public void parseXml(String xml) {
        VICTIM_DATA.setSupporterUrl("");
        VICTIM_DATA.setRadius(Long.valueOf(0));
        VICTIM_DATA.setLocation(new Location(""));
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(new StringReader(xml));
            int eventType = parser.getEventType();
            String name = "";
            while (eventType != XmlPullParser.END_DOCUMENT) {
                switch (eventType) {
                    case XmlPullParser.START_TAG:
                        name = parser.getName();
                        break;
                    case XmlPullParser.TEXT:
                        String text = parser.getText();
                        System.out.println("name: " + name + "  text: " + text);
                        if (name.equalsIgnoreCase("support"))
                            VICTIM_DATA.setSupporterUrl(parser.getText());
                        else if (name.equalsIgnoreCase("radius"))
                            VICTIM_DATA.setRadius(Long.valueOf(parser.getText()));
                        else if (name.equalsIgnoreCase("latitude"))
                            VICTIM_DATA.getLocation().setLatitude(Double.valueOf(parser.getText()));
                        else if (name.equalsIgnoreCase("longitude"))
                            VICTIM_DATA.getLocation().setLongitude(Double.valueOf(parser.getText()));
                        else if (name.equalsIgnoreCase("text"))
                            VICTIM_DATA.setLastComment(parser.getText());
                        else if (name.equalsIgnoreCase("full_name"))
                            VICTIM_DATA.setName(parser.getText());
                        break;
                }
                eventType = parser.next();
            }
            System.out.println("End document");

            System.out.println("support_url: " + VICTIM_DATA.getSupporterUrl());
            System.out.println("radius: " + VICTIM_DATA.getRadius());
            System.out.println("loc: " + VICTIM_DATA.getLocation().toString());
            System.out.println("victim name: " + VICTIM_DATA.getName());
            System.out.println("last comment: " + VICTIM_DATA.getLastComment());

        } catch (Exception e) {}
    }

    private boolean isSubscribed(Node node, String user_jid) {
        boolean result = false;
        try {
            for (Subscription s: node.getSubscriptions()) {
                Log.i(TAG, "subscription: " + s.getJid());
                if (s.getJid().equalsIgnoreCase(user_jid) && s.getState().equals(Subscription.State.subscribed)) {
                    result = true;
                    break;
                }
            }
        } catch (Exception e) {}

        logs.info("checking if user subscribet to xmpp pubsub node: " + String.valueOf(result));
        return result;
    }

    public void openAcceptEventScreen() {
        logs.info("opening AcceptEvent screen");
        Log.i("XMPP", "opening AcceptEventScreen");
        Intent i = new Intent(this, SosActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); // fix for navigation bug
        i.putExtra(SosActivity.FRAGMENT_KEY, AcceptEventFragment.class.getName());
        startActivity(i);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            if (mSmackAndroid != null)
                mSmackAndroid.onDestroy();
            mConnection.removePacketListener(mPacketListener);
            mNode.removeItemEventListener(mItemEventListener);
            mConnection.disconnect();
            processing_event = false;
            logs.info("XmppService stopped");
            logs.close();
        } catch (Exception e) {}
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
