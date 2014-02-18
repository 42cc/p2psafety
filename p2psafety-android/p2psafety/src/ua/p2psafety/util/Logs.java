package ua.p2psafety.util;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Created by Taras Melon on 17.02.14.
 */
public class Logs {

    private static final String FILENAME = "/logs_";
    private static final String FILE_EXTENSION = ".txt";

    private Context mContext;
    private PrintWriter mWriter;

    private String mFullFileName;

    public Logs(Context context)
    {
        mContext = context;

        open();

        checkLogsFiles();
    }

    private void checkLogsFiles() {
        DateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd");
        boolean shouldWork = true;
        //one week
        int i=-7;

        File mediaDir = checkForSdCard();

        while (shouldWork)
        {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DATE, i);
            String date = dateFormat.format(cal.getTime());

            String filename = FILENAME+date+FILE_EXTENSION;

            mFullFileName = mediaDir+filename;

            File file = new File(mFullFileName);
            if(file.exists())
            {
                file.delete();
            }
            else
            {
                shouldWork = false;
            }

            i--;
        }
    }

    private File checkForSdCard() {
        File mediaDir;
        String state = Environment.getExternalStorageState();
        if(state.equals(Environment.MEDIA_MOUNTED))
            mediaDir = Environment.getExternalStorageDirectory();
        else
            mediaDir = mContext.getFilesDir();
        return mediaDir;
    }

    public synchronized void debug(String message)
    {
        log(Level.DEBUG, message, null);
    }

    public synchronized void debug(String message, Throwable throwable)
    {
        log(Level.DEBUG, message, throwable);
    }

    public synchronized void error(String message)
    {
        log(Level.ERROR, message, null);
    }

    public synchronized void error(String message, Throwable throwable)
    {
        log(Level.ERROR, message, throwable);
    }

    public synchronized void fatal(String message)
    {
        log(Level.FATAL, message, null);
    }

    public synchronized void fatal(String message, Throwable throwable)
    {
        log(Level.FATAL, message, throwable);
    }

    public synchronized void warn(String message)
    {
        log(Level.WARN, message, null);
    }

    public synchronized void warn(String message, Throwable throwable)
    {
        log(Level.WARN, message, throwable);
    }

    public synchronized void info(String message)
    {
        log(Level.INFO, message, null);
    }

    public synchronized void info(String message, Throwable throwable)
    {
        log(Level.INFO, message, throwable);
    }

    private synchronized void log(Level level, String message, Throwable throwable)
    {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String currentDateandTime = sdf.format(new Date());

        StringBuilder builder = new StringBuilder().append(currentDateandTime).append(" "+level.toString()).append(": "+message);

        if (throwable != null) {
            throwable.printStackTrace();
            saveToFile(builder.toString(), throwable);
        }
        else
        {
            saveToFile(builder.toString());
        }
    }

    private synchronized void saveToFile(String fullMessage)
    {
        if (mWriter != null) {
            mWriter.println(fullMessage);
            mWriter.flush();
        }
    }

    private synchronized void saveToFile(String fullMessage, Throwable throwable)
    {
        if (mWriter != null) {
            mWriter.println(fullMessage);
            throwable.printStackTrace(mWriter);
            mWriter.flush();
        }
    }

    private synchronized void open()
    {
        File logFile = getLogFile();

        if (logFile != null) {
            if (!logFile.exists()) {
                try {
                    if(!logFile.createNewFile()) {
                        Log.e("Logs", "Unable to create new log file");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            FileOutputStream fileOutputStream = null;
            try {
                fileOutputStream = new FileOutputStream(logFile, true);

                if(fileOutputStream != null) {
                    mWriter = new PrintWriter(fileOutputStream);
                } else {
                    Log.e("Logs", "Failed to create the log file (no stream)");
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    private synchronized File getLogFile()
    {
        File mediaDir = checkForSdCard();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy_MM_dd");
        String currentDate = sdf.format(new Date());

        String filename = FILENAME+currentDate+FILE_EXTENSION;

        mFullFileName = mediaDir+filename;

        File file = new File(mFullFileName);
        if(file.exists())
        {
            return file;
        }
        else
        {
            try {
                file.createNewFile();
                return file;
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    public void close()
    {
        if (mWriter != null)
            mWriter.close();
    }

    public String getFullFileName()
    {
        return mFullFileName;
    }

    public List<File> getFiles() {
        List<File> files = new ArrayList<File>();

        DateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd");
        boolean shouldWork = true;
        //today
        int i=0;

        File mediaDir = checkForSdCard();

        while (shouldWork)
        {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DATE, i);
            String date = dateFormat.format(cal.getTime());

            String filename = FILENAME+date+FILE_EXTENSION;

            mFullFileName = mediaDir+filename;

            File file = new File(mFullFileName);
            if(file.exists())
            {
                files.add(file);
            }
            else
            {
                shouldWork = false;
            }

            i--;
        }

        return files;
    }
}
