package ua.p2psafety.sms;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import com.sun.mail.smtp.SMTPTransport;
import com.sun.mail.util.BASE64EncoderStream;

import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.URLName;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import ua.p2psafety.util.Utils;

/**
 * From http://stackoverflow.com/questions/12503303/javamail-api-in-android-using-xoauth
 */
public class GmailOAuth2Sender {

    private Session session;
    private String token;
    private AccountManager mAccountManager;
    private Context context;

    public GmailOAuth2Sender(Context ctx) {
        super();
        context = ctx;
        initToken();
    }

    private SMTPTransport connectToSmtp(String host, int port, String userEmail,
                                        String oauthToken, boolean debug) throws MessagingException {

        Properties props = new Properties();
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");
        props.put("mail.smtp.sasl.enable", "false");
        props.put("mail.smtp.ssl.enable", true);
        session = Session.getInstance(props);
        session.setDebug(debug);
        final URLName unusedUrlName = null;
        SMTPTransport transport = new SMTPTransport(session, unusedUrlName);
        // If the password is non-null, SMTP tries to do AUTH LOGIN.
        final String emptyPassword = null;
        transport.connect(host, port, userEmail, emptyPassword);

        byte[] response = String.format("user=%s\1auth=Bearer %s\1\1", userEmail,
                oauthToken).getBytes();
        response = BASE64EncoderStream.encode(response);

        transport.issueCommand("AUTH XOAUTH2 " + new String(response),
                235);

        return transport;
    }

    @SuppressLint("NewApi")
    public void initToken() {

        mAccountManager = AccountManager.get(context);

        Account[] accounts = mAccountManager.getAccountsByType("com.google");
        for (Account account : accounts) {
            Log.d("getToken", "account=" + account);
        }

        Account me = accounts[0]; //You need to get a google account on the device, it changes if you have more than one
        int currentApiVersion = android.os.Build.VERSION.SDK_INT;
        if (currentApiVersion >= Build.VERSION_CODES.ICE_CREAM_SANDWICH){
            mAccountManager.getAuthToken(me, "oauth2:https://mail.google.com/", null,
                    false, new AccountManagerCallback<Bundle>() {
                @Override
                public void run(AccountManagerFuture<Bundle> result) {
                    saveToken(result);
                }
            }, null);
        } else {
            mAccountManager.getAuthToken(me, "oauth2:https://mail.google.com/",
                    false, new AccountManagerCallback<Bundle>() {
                @Override
                public void run(AccountManagerFuture<Bundle> result) {
                    saveToken(result);
                }
            }, null);
        }

        Log.d("getToken", "token=" + token);
    }

    private void saveToken(AccountManagerFuture<Bundle> result) {
        try {
            Bundle bundle = result.getResult();
            token = bundle.getString(AccountManager.KEY_AUTHTOKEN);
            Log.d("initToken callback", "token=" + token);

        } catch (Exception e) {
            Log.d("test", e.getMessage());
        }
    }

    public synchronized void sendMail(String subject, String body, String user, String recipients) {
        if (!Utils.isNetworkConnected(context)) {
            return;
        }
        SMTPTransport smtpTransport = null;
        try {
            smtpTransport = connectToSmtp("smtp.gmail.com",
                    587,
                    user,
                    token,
                    true);

            MimeMessage message = new MimeMessage(session);
            message.setSender(new InternetAddress(user));
            message.setSubject(subject);
            message.setContent(body, "text/html; charset=utf-8");

            try {
                if (recipients.indexOf(',') > 0)
                    message.setRecipients(Message.RecipientType.TO,
                            InternetAddress.parse(recipients));
                else
                    message.setRecipient(Message.RecipientType.TO,
                            new InternetAddress(recipients));
                smtpTransport.sendMessage(message, message.getAllRecipients());
            } finally {
                smtpTransport.close();
            }
        } catch (MessagingException e) {
            mAccountManager.invalidateAuthToken("com.google", token);
            initToken();
            sendMail(subject, body, user, recipients);
        }
    }
}
