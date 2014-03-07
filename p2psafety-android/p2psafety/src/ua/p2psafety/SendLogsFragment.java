package ua.p2psafety;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.List;

import ua.p2psafety.sms.GmailOAuth2Sender;
import ua.p2psafety.util.Utils;

/**
 * Created by Taras Melon on 18.02.14.
 */
public class SendLogsFragment extends Fragment {

    public static final String TAG = "SendLogsFragment";

    private View vParent;
    private Activity mActivity;

    public SendLogsFragment() {
        super();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.frag_send_logs, container, false);
        ((SosActivity) getActivity()).getSupportActionBar().setHomeButtonEnabled(true);
        ((SosActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        vParent = rootView;

        mActivity = getActivity();

        final TextView descriptionOfIssue = (TextView) rootView.findViewById(R.id.description_of_issue);

        rootView.findViewById(R.id.send_logs_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = descriptionOfIssue.getText().toString();
                if (!text.equals(""))
                {
                    Utils.setLoading(mActivity, true);
                    List<File> files = SosActivity.mLogs.getFiles();
                    AsyncTaskExecutionHelper.executeParallel(new SendReportAsyncTask(files, text));
                    //return to settings
                    mActivity.onBackPressed();
                }
                else {
                    Toast.makeText(mActivity, R.string.please_write_a_description_of_issue,
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();

        getView().bringToFront();
    }

    private class SendReportAsyncTask extends AsyncTask<Void, Void, Boolean> {

        private List<File> files;
        private String message;
        private final String email = getString(R.string.email_for_logs);

        public SendReportAsyncTask(List<File> files, String message)
        {
            this.files = files;
            this.message = message;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            String account = Utils.getEmail(mActivity);
            if (account != null) {
                GmailOAuth2Sender gmailOAuth2Sender = new GmailOAuth2Sender(mActivity);
                gmailOAuth2Sender.sendMail("[p2psafety] Report an issue",
                        "Logs in attachments. Description of issue:\r\n" + message, account,
                        email, files);
                return true;
            }
            else {
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean o) {
            super.onPostExecute(o);

            if (o) {
                Toast.makeText(mActivity, R.string.logs_successfully_sent, Toast.LENGTH_SHORT)
                     .show();
                // delete sent logs from device
                for (File f: files)
                    f.delete();
            }
            else
                Toast.makeText(mActivity, R.string.no_account_message, Toast.LENGTH_SHORT)
                     .show();

            Utils.setLoading(mActivity, false);
        }
    }

}
