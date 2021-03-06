/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.netbeans.modules.remotefs.sftp;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyVetoException;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.logging.Logger;
import org.netbeans.modules.remotefs.api.LogInfo;
import org.netbeans.modules.remotefs.api.RemoteClient;
import org.netbeans.modules.remotefs.api.RemoteFileSystem;
import org.netbeans.modules.remotefs.sftp.client.SFTPClient;
import org.netbeans.modules.remotefs.sftp.client.SFTPLogInfo;
import org.netbeans.modules.remotefs.sftp.resources.Bundle;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.filesystems.FileObject;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;

/**
 *
 * @author hlavki
 */
public class SFTPFileSystem extends RemoteFileSystem implements SFTPClient.Reconnect {

    private static final long serialVersionUID = 1L;
    private static final String PROP_NOTIFY_RECONNECT_MESSAGE = "SFTPFileSystem.notifyReconnect.message";
    private static final String PROP_NOTIFY_RECONNECT_TITLE = "SFTPFileSystem.notifyReconnect.message";
    private static final String PROP_WHICH_FILE_OPT_PREFIX = "SFTPFileSystem.whichFile.opt.prefix";
    private static final String PROP_WHICH_FILE_OPT_0 = "SFTPFileSystem.whichFile.opt.0";
    private static final String PROP_WHICH_FILE_OPT_1 = "SFTPFileSystem.whichFile.opt.1";
    private static final String PROP_WHICH_FILE_TITLE = "SFTPFileSystem.whichFile.label.message";
    private static final String PROP_WHICH_FILE_LBL_MESSAGE = "SFTPFileSystem.whichFile.label.message";
    private static final String PROP_WHICH_FILE_LBL_QUESTION = "SFTPFileSystem.whichFile.label.question";
    private static final String PROP_WHICH_FILE_LBL_WARN = "SFTPFileSystem.whichFile.label.warn";
    private static final String PROP_WHICH_FILE_LBL_INFO = "SFTPFileSystem.whichFile.label.fileInfo";
    private static final String PROP_ASK_AGAIN_QUESTION = "SFTPFileSystem.askAgainQuestion";
    private static final String PROP_BOTH_FILES_OPT_PREFIX = "SFTPFileSystem.bothFiles.opt.prefix";
    private static final String PROP_BOTH_FILES_OPT_0 = "SFTPFileSystem.bothFiles.opt.0";
    private static final String PROP_BOTH_FILES_OPT_1 = "SFTPFileSystem.bothFiles.opt.1";
    private static final Logger log = Logger.getLogger(SFTPFileSystem.class.getName());
    /** Global FTP FileSystem settings */
    private SFTPSettings settings;

    public SFTPFileSystem(SFTPLogInfo logInfo) {
        super(logInfo);
        this.settings = SFTPSettings.getDefault();
        setRefreshTime(getSFTPSettings().getRefreshTime());
        startDir = logInfo.getRootFolder();
        getSFTPSettings().addPropertyChangeListener(new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent event) {
                sftpSettingsChanged(event);
            }
        });
    }

    /** Get server name.
     * @return Value of property server.
     */
    public String getServer() {
        return ((SFTPLogInfo) logInfo).getHost();
    }

    /** Set server name.
     */
    public void setServer(String host) {
        ((SFTPLogInfo) logInfo).setHost(host);
    // TODO: property changed
    }

    /** Get the number of port.
     * @return Value of property port.
     */
    public int getPort() {
        return ((SFTPLogInfo) logInfo).getPort();
    }

    /** Set port number.
     * @param port New value of property port.
     * @throws PropertyVetoException
     */
    public void setPort(int port) throws java.beans.PropertyVetoException {
        ((SFTPLogInfo) logInfo).setPort(port);
    // TODO: property changed
    }

    /** Get user name.
     * @return Value of property username.
     */
    public String getUsername() {
        return ((SFTPLogInfo) logInfo).getUser();
    }

    /** Set user name.
     * @param username New value of property username.
     * @throws java.beans.PropertyVetoException
     */
    public void setUsername(String username) throws PropertyVetoException {
        ((SFTPLogInfo) logInfo).setUser(username);
    // TODO: property changed
    }

    /** Get password.
     * @return Value of property password.
     */
    public String getPassword() {
        return ((SFTPLogInfo) logInfo).getPassword();
    }

    /** Set password.
     * @param password
     * @throws java.beans.PropertyVetoException
     */
    public void setPassword(String password) throws PropertyVetoException {
        ((SFTPLogInfo) logInfo).setPassword(password);
    // TODO: property changed
    }

    /** Get starting directory.
     * @return Value of property startdir.
     */
    public String getStartDir() {
        return startDir;
    }

    /** Set starting directory.
     * @param startdir New value of property startdir.
     */
    public void setStartDir(String startDir) {
        String newStartDir = startDir;
        if (startDir == null || startDir.equals("/") || startDir.equals("")) {
            newStartDir = "/";
        } else {
            if (!startDir.startsWith("/") && !startDir.startsWith(".")) {
                newStartDir = "/" + startDir;
            }
            if (newStartDir.endsWith("/")) {
                newStartDir = newStartDir.substring(0, newStartDir.length() - 1);
            }
        }
        this.startDir = newStartDir;
        ((SFTPLogInfo) logInfo).setRootFolder(newStartDir);
        removeClient();
    }

    /** Get SFTPSettings object
     * @return 
     */
    protected SFTPSettings getSFTPSettings() {
        return settings;
    }

    private String computeSystemName() {
        return logInfo.getDisplayName();
    }

    private void sftpSettingsChanged(PropertyChangeEvent event) {
        log.fine("SFTP settings changed...");
        if (event.getPropertyName().equals(SFTPSettings.PROP_REFRESH_TIME)) {
            setRefreshTime(((Integer) (event.getNewValue())).intValue());
        }
    }

    @Override
    public RemoteClient createClient(LogInfo loginfo, FileObject cache) throws IOException {
        SFTPClient sftpClient = new SFTPClient((SFTPLogInfo) loginfo);
        sftpClient.setReconnect(this);
        return sftpClient;
    }

    /** Test whether filesystem is ready to write. If no, throws exception
     * @throws java.io.IOException 
     */
    protected void isReadyToModify() throws IOException {
        if (client == null || rootFile == null) {
            throw new IOException("Connection to server " + getServer() + " isn't established");
        }
        if (!isConnected() && !isOfflineChanges()) {
            throw new IOException("Modification in offline mode are not allowed");
        }
    }

    /** Test whether filesystem is ready to read. If no, throws exception
     * @throws java.io.IOException 
     */
    protected void isReadyToRead() throws IOException {
        if (client == null || rootFile == null) {
            throw new IOException("Connection to server " + getServer() + " isn't established");
        }
    }

    /** Test whether filesystem is ready. */
    protected boolean isReady() {
        if (client == null || rootFile == null) {
            return false;
        } else {
            return true;
        }
    }

    protected int disconnectDialog(String server) {
        return SFTPDialogs.disconnect(server);
    }

    protected boolean connectDialog(String server) {
        return SFTPDialogs.connect(server);
    }

    protected void startdirNotFound(String startdir, String server) {
        SFTPDialogs.startdirNotFound(startdir, server);
    }

    protected void errorConnect(String error) {
        SFTPDialogs.errorConnect(error);
    }

    public void notifyIncorrectPassword() {
        SFTPDialogs.incorrectPassword(getServer());
    }

    public boolean notifyIncorrectCache(java.io.File newcache) {
        return SFTPDialogs.incorrectCache(getCache().getPath(), newcache.getPath(), getServer());
    }

    @Override
    public String getDisplayName() {
        return computeSystemName();
    }

    public boolean notifyReconnect(String message) {
        Object obj = DialogDisplayer.getDefault().notify(new NotifyDescriptor(
                NbBundle.getMessage(Bundle.class, PROP_NOTIFY_RECONNECT_MESSAGE, new Object[]{getServer(), message}),
                NbBundle.getMessage(Bundle.class, PROP_NOTIFY_RECONNECT_TITLE),
                NotifyDescriptor.YES_NO_OPTION, NotifyDescriptor.QUESTION_MESSAGE, null, null));
        if (obj == NotifyDescriptor.YES_OPTION) {
            return true;
        } else {
            return false;
        }
    }

    public int notifyWhichFile(String path, Date file1, long size1, Date file2, long size2) {
        int which = file1.before(file2) ? 0 : 1;
        if (!getSFTPSettings().isAskWhichFile()) {
            return which;
        }
        Object ops[] = new String[2];
        String optPrefix = NbBundle.getMessage(Bundle.class, PROP_WHICH_FILE_OPT_PREFIX);
        ops[0] = optPrefix + NbBundle.getMessage(Bundle.class, PROP_WHICH_FILE_OPT_0);
        ops[1] = optPrefix + NbBundle.getMessage(Bundle.class, PROP_WHICH_FILE_OPT_1);
        javax.swing.JPanel panel = new javax.swing.JPanel(new java.awt.BorderLayout());
        javax.swing.JPanel textpanel = new javax.swing.JPanel(new java.awt.GridLayout(0, 1));
        textpanel.add(new javax.swing.JLabel(NbBundle.getMessage(Bundle.class, PROP_WHICH_FILE_LBL_MESSAGE)));
        textpanel.add(new javax.swing.JLabel(NbBundle.getMessage(Bundle.class, PROP_WHICH_FILE_LBL_INFO,
                new Object[]{computeSystemName().substring(0, computeSystemName().length() - 1) + path, size1, file1.toString()})));
        textpanel.add(new javax.swing.JLabel(NbBundle.getMessage(Bundle.class, PROP_WHICH_FILE_LBL_INFO,
                new Object[]{getCache().getPath() + path.replace('/', File.separatorChar), size2, file2.toString()})));
        textpanel.add(new javax.swing.JLabel(NbBundle.getMessage(Bundle.class, PROP_WHICH_FILE_LBL_QUESTION,
                new Object[]{which == 0 ? ops[0] : ops[1]})));
        textpanel.add(new javax.swing.JLabel(NbBundle.getMessage(Bundle.class, PROP_WHICH_FILE_LBL_WARN)));
        panel.add(textpanel, java.awt.BorderLayout.NORTH);
        javax.swing.JCheckBox chbox = new javax.swing.JCheckBox(NbBundle.getMessage(Bundle.class, PROP_ASK_AGAIN_QUESTION));
        chbox.setSelected(false);
        panel.add(chbox);
        Object obj = DialogDisplayer.getDefault().notify(new NotifyDescriptor(panel,
                NbBundle.getMessage(Bundle.class, PROP_WHICH_FILE_TITLE),
                NotifyDescriptor.YES_NO_OPTION, NotifyDescriptor.QUESTION_MESSAGE, ops, ops[which]));
        if (chbox.isSelected()) {
            getSFTPSettings().setAskWhichFile(false);
        }
        if (obj.equals(ops[0])) {
            return 0;
        } else {
            return 1;
        }
    }

    public int notifyBothFilesChanged(String path, Date file1, long size1, Date file2, long size2) {
        Object ops[] = new String[2];
        String optPrefix = NbBundle.getMessage(Bundle.class, PROP_BOTH_FILES_OPT_PREFIX);
        ops[0] = optPrefix + NbBundle.getMessage(Bundle.class, PROP_BOTH_FILES_OPT_0);
        ops[1] = optPrefix + NbBundle.getMessage(Bundle.class, PROP_BOTH_FILES_OPT_1);
        int which = file1.before(file2) ? 0 : 1;
        //TODO: better message (branch, merge ...)
        Object obj = DialogDisplayer.getDefault().notify(new NotifyDescriptor(
                "Both files in FTP server and in cache were modified. It means that two diffrent version of this file exist.\n" +
                computeSystemName().substring(0, computeSystemName().length() - 1) + path + ", size " + size1 + " bytes, last modified " + file1.toString() + "\n" +
                getCache().getPath() + path.replace('/', File.separatorChar) + ", size " + size2 + " bytes, last modified " + file2.toString() + "\n" +
                "File in " + (which == 0 ? "cache" : "FTP server") + " seems to be newer. Which one do you want to use?\n" +
                "Attention: second one will be deleted!",
                "Files changed", NotifyDescriptor.YES_NO_OPTION, NotifyDescriptor.WARNING_MESSAGE, ops, ops[which]));
        if (obj == ops[0]) {
            return 0;
        } else {
            return 1;
        }
    }

    public boolean isRefreshServer() {
        return getSFTPSettings().isRefreshServer();
    }

    public boolean isScanCache() {
        return getSFTPSettings().isScanCache();
    }

    public boolean isAlwaysRefresh() {
        return getSFTPSettings().isAlwaysRefresh();
    }

    public void setAlwaysRefresh(boolean alwaysRefresh) {
        getSFTPSettings().setAlwaysRefresh(alwaysRefresh);
    }

    public boolean isDownloadServerChangedFile() {
        return getSFTPSettings().isDownloadServerChangedFile();
    }

    public boolean isOfflineChanges() {
        return getSFTPSettings().isOfflineChanges();
    }

    public boolean notifyServerChanged(String path, Date file1, long size1, Date file2, long size2) {
        if (!getSFTPSettings().isAskServerChangedFile()) {
            return true;
        } // I agree
        javax.swing.JPanel panel = new javax.swing.JPanel(new java.awt.BorderLayout());
        javax.swing.JPanel textpanel = new javax.swing.JPanel(new java.awt.GridLayout(0, 1));
        textpanel.add(new javax.swing.JLabel("I detected that the file in FTP server has been changed."));
        textpanel.add(new javax.swing.JLabel(computeSystemName().substring(0, computeSystemName().length() - 1) + path + ", size " + size1 + " bytes, last modified " + file1.toString()));
        textpanel.add(new javax.swing.JLabel(getCache().getPath().replace('/', File.separatorChar) + path + ", size " + size2 + " bytes, last modified " + file2.toString()));
        textpanel.add(new javax.swing.JLabel("I will use this new file from server and delete the file in cache. Do you agree?"));
        textpanel.add(new javax.swing.JLabel("If you say No, the file from cache will be upload to server over changed one."));
        panel.add(textpanel, java.awt.BorderLayout.NORTH);
        javax.swing.JCheckBox chbox = new javax.swing.JCheckBox("Don't ask again. Always use new file from server");
        chbox.setSelected(false);
        panel.add(chbox);
        Object obj = DialogDisplayer.getDefault().notify(new NotifyDescriptor(panel,
                "Question", NotifyDescriptor.YES_NO_OPTION, NotifyDescriptor.QUESTION_MESSAGE, null, NotifyDescriptor.YES_OPTION));
        if (chbox.isSelected()) {
            getSFTPSettings().setAskServerChangedFile(false);
        }
        if (obj == NotifyDescriptor.YES_OPTION) {
            return true;
        } else {
            return false;
        }
    }

    public boolean notifyCacheExtDelete(String path, boolean isDir) {
        if (!getSFTPSettings().isAskCacheExternalDelete()) {
            return getSFTPSettings().isCacheExternalDelete();
        }
        Object ops[] = new String[4];
        ops[0] = "Yes";
        ops[1] = "No";
        ops[2] = "Yes for All";
        ops[3] = "No for All";
        Object obj = DialogDisplayer.getDefault().notify(new NotifyDescriptor(
                (isDir ? "The directory " + path + " in cache was delete externally.\nDo you want to the delete directory and all the subdirectories also from server?\n" : "The file " + path + " in cache was delete externaly.\nDo you want to delete the file also from server?\n"),
                "External deletion", NotifyDescriptor.YES_NO_OPTION, NotifyDescriptor.QUESTION_MESSAGE, ops, ops[1]));
        if (obj == ops[2]) {
            getSFTPSettings().setAskCacheExternalDelete(false);
            getSFTPSettings().setCacheExternalDelete(true);
        }
        if (obj == ops[3]) {
            getSFTPSettings().setAskCacheExternalDelete(false);
            getSFTPSettings().setCacheExternalDelete(false);
        }
        if (obj == ops[0] || obj == ops[2]) {
            return true;
        } else {
            return false;
        }
    }

    public boolean notifyServerExtDelete(String path, boolean isDir) {
        if (!getSFTPSettings().isAskServerExternalDelete()) {
            return getSFTPSettings().isServerExternalDelete();
        }
        Object ops[] = new String[4];
        ops[0] = "Yes";
        ops[1] = "No";
        ops[2] = "Yes for All";
        ops[3] = "No for All";
        Object obj = DialogDisplayer.getDefault().notify(new NotifyDescriptor(
                (isDir ? "The directory " + path + " on server was delete externally.\nDo you want to delete the directory and all the subdirectories also from cache?\n" : "The file " + path + " on server was delete externally.\nDo you want to delete the file also from cache?\n"),
                "External deletion", NotifyDescriptor.YES_NO_OPTION, NotifyDescriptor.QUESTION_MESSAGE, ops, ops[1]));
        if (obj == ops[2]) {
            getSFTPSettings().setAskServerExternalDelete(false);
            getSFTPSettings().setServerExternalDelete(true);
        }
        if (obj == ops[3]) {
            getSFTPSettings().setAskServerExternalDelete(false);
            getSFTPSettings().setServerExternalDelete(false);
        }
        if (obj == ops[0] || obj == ops[2]) {
            return true;
        } else {
            return false;
        }
    }

    public void fileChanged(String path) {
        FileObject fo = findResource(path);
        if (fo != null) {
            fo.refresh();
        }
    }

    public void notifyException(Exception e) {
        Exceptions.printStackTrace(e);
    }
}
