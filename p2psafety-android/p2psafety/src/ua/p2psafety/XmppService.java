package ua.p2psafety;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import org.jivesoftware.smack.AndroidConnectionConfiguration;
import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.SmackAndroid;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.MessageTypeFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smackx.packet.DiscoverItems;
import org.jivesoftware.smackx.pubsub.ItemPublishEvent;
import org.jivesoftware.smackx.pubsub.LeafNode;
import org.jivesoftware.smackx.pubsub.Node;
import org.jivesoftware.smackx.pubsub.PubSubManager;
import org.jivesoftware.smackx.pubsub.Subscription;
import org.jivesoftware.smackx.pubsub.listener.ItemEventListener;

import java.io.File;
import java.util.Iterator;

public class XmppService extends Service {
    private static final String TAG = "XmppService";

    private static final String HOST = "p2psafety.net";

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        connectToServer();
        return Service.START_STICKY;
    }

    private void connectToServer() {
        SmackAndroid.init(this);
        XMPPConnection connection = getConfiguredConnection(HOST);

        try {
            connection.connect();
            connection.login("Uvs", "RandomPassword");
        } catch (Exception e) {
            Log.i(TAG, "Error during connection");
            e.printStackTrace();
            return;
        }

        setMessageListener(connection);
        setPubsubListener(connection);
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
    private void setMessageListener(Connection connection) {
        connection.addPacketListener(new PacketListener() {
            public void processPacket(Packet packet) {
                Message mes = (Message) packet;
                Log.i("got personal message", mes.toXML());
            }
        }, null); // new MessageTypeFilter(Message.Type.chat)

        // TODO look at 'accept()' method
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

            LeafNode testNode = pbManager.getNode("Uvs");
            testNode.addItemEventListener(new ItemEventListener() {
                @Override
                public void handlePublishedItems(ItemPublishEvent items) {
                    Log.i("got pubsub message", "Item count: " + items.getItems().size());
                    Log.i("got pubsub message", items.toString());
                    Log.i("got pubsub message", items.getItems().get(0).toString());
                }
            });

            if (!isSubscribed(testNode, "Uvs@p2psafety.net")) {
                Log.i(TAG, "making new subscription");
                testNode.subscribe("Uvs@p2psafety.net");
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
            e.printStackTrace();
        }
    }

    private boolean isSubscribed(Node node, String user_jid) {
        boolean result = false;
        try {
            for (Subscription s: node.getSubscriptions()) {
                Log.i(TAG, "subscription: " + s.getJid());
                if (s.getJid().toLowerCase().equals(user_jid.toLowerCase()) && s.getState().equals(Subscription.State.subscribed)) {
                    result = true;
                    break;
                }
            }
        } catch (Exception e) {}

        return result;
    }

    @Override
    public void onDestroy() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
