import React, { createContext, useContext, useState, useEffect, ReactNode, useCallback } from 'react';
import { 
  UserProfile, 
  UserRoleCode, 
  TelecomSite, 
  SiteReport, 
  SiteAccessStatus, 
  SitePhoto, 
  GpsData, 
  GsmData, 
  MailConfig 
} from '../types';
import { StorageService } from '../services/storage';
import { TelemetryService } from '../services/telemetry';
import { EmailService } from '../services/email';
import { SupabaseSiteService, isSupabaseConfigured, SUPABASE_URL } from '../services/supabase';

interface AppContextType {
  // Auth & Session
  currentUser: UserProfile | null;
  currentRole: UserRoleCode;
  isPendingActivation: boolean;
  signIn: (email: string, password?: string) => Promise<{ success: boolean; message?: string }>;
  signUp: (data: { name: string; email: string; phone: string; department: string; region: string; role: UserRoleCode }) => Promise<{ success: boolean; message: string }>;
  signOut: () => void;
  switchRole: (role: UserRoleCode) => void;

  // Navigation
  activeTab: 'AUDIT' | 'MAP' | 'HISTORY' | 'SETTINGS';
  setActiveTab: (tab: 'AUDIT' | 'MAP' | 'HISTORY' | 'SETTINGS') => void;

  // Online / Queue status
  isOnline: boolean;
  pendingCount: number;
  isSyncingQueue: boolean;
  syncPendingReports: () => Promise<void>;

  // Form State
  siteId: string;
  setSiteId: (val: string) => void;
  siteName: string;
  setSiteName: (val: string) => void;
  operator: string;
  setOperator: (val: string) => void;
  status: SiteAccessStatus;
  setStatus: (val: SiteAccessStatus) => void;
  refusalReason: string;
  setRefusalReason: (val: string) => void;
  notes: string;
  setNotes: (val: string) => void;
  technicianName: string;
  setTechnicianName: (val: string) => void;
  technicianPhone: string;
  setTechnicianPhone: (val: string) => void;
  recipientEmail: string;
  setRecipientEmail: (val: string) => void;
  ccRecipientsEmail: string;
  setCcRecipientsEmail: (val: string) => void;

  // Sensors & Telemetry
  gpsData: GpsData;
  isRefreshingGps: boolean;
  refreshLocation: () => Promise<void>;
  gsmData: GsmData;
  isRefreshingGsm: boolean;
  refreshGsm: () => void;

  // Photos
  photos: SitePhoto[];
  addPhoto: (photo: SitePhoto) => void;
  removePhoto: (id: string) => void;
  isCameraOpen: boolean;
  openCamera: (tag?: string) => void;
  closeCamera: () => void;
  activeCameraTag: string;
  setActiveCameraTag: (tag: string) => void;

  // Email Preview & Send
  showEmailPreview: boolean;
  setShowEmailPreview: (val: boolean) => void;
  isSendingEmail: boolean;
  sendResultDialog: { show: boolean; success: boolean; title: string; message: string; details?: string };
  closeSendResultDialog: () => void;
  sendAuditReport: () => Promise<void>;
  saveAsDraftQueue: () => void;

  // Sites Directory & Supabase site_db
  telecomSites: TelecomSite[];
  isSyncingSites: boolean;
  supabaseSitesStatus: string | null;
  supabaseUrl: string;
  isSupabaseConnected: boolean;
  fetchSitesFromSupabase: (isManual?: boolean) => Promise<void>;
  selectedSiteForMap: TelecomSite | null;
  selectSiteForMap: (site: TelecomSite | null) => void;
  loadSiteIntoAudit: (site: TelecomSite) => void;
  addNewTelecomSite: (site: Omit<TelecomSite, 'id'>) => void;

  // History & Reports
  reports: SiteReport[];
  loadReportIntoAudit: (report: SiteReport) => void;
  deleteReport: (id: string) => void;

  // Admin Mail Config & User Directory
  mailConfig: MailConfig;
  updateMailConfig: (config: Partial<MailConfig>) => void;
  allUsers: UserProfile[];
  pendingUsers: UserProfile[];
  approveUser: (id: string) => void;
  rejectUser: (id: string) => void;
  updateUserRole: (id: string, role: UserRoleCode) => void;
  addUserDirectly: (user: Omit<UserProfile, 'id' | 'createdAt'>) => void;

  // Toast / User notification
  toastMessage: string | null;
  showToast: (msg: string) => void;
  clearToast: () => void;
}

const AppContext = createContext<AppContextType | undefined>(undefined);

export const AppProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  // Current user & Auth
  const [currentUser, setCurrentUser] = useState<UserProfile | null>(() => StorageService.getCurrentUser());
  const [isPendingActivation, setIsPendingActivation] = useState<boolean>(() => {
    const user = StorageService.getCurrentUser();
    return user ? !user.isActive : false;
  });

  const currentRole: UserRoleCode = currentUser?.role || 'TECHNICIAN';

  // Navigation
  const [activeTab, setActiveTab] = useState<'AUDIT' | 'MAP' | 'HISTORY' | 'SETTINGS'>('AUDIT');

  // Network status
  const [isOnline, setIsOnline] = useState<boolean>(navigator.onLine);
  useEffect(() => {
    const handleOnline = () => {
      setIsOnline(true);
      showToast('Internet connectivity restored.');
    };
    const handleOffline = () => {
      setIsOnline(false);
      showToast('Network offline. Audits will be queued locally.');
    };
    window.addEventListener('online', handleOnline);
    window.addEventListener('offline', handleOffline);
    return () => {
      window.removeEventListener('online', handleOnline);
      window.removeEventListener('offline', handleOffline);
    };
  }, []);

  // Form Fields
  const [siteId, setSiteId] = useState('MOB-ALG-1044');
  const [siteName, setSiteName] = useState('Hydra Telecom Tower');
  const [operator, setOperator] = useState('Mobiserve / Djezzy');
  const [status, setStatus] = useState<SiteAccessStatus>('ACCORDED');
  const [refusalReason, setRefusalReason] = useState('Site security guard requested regional work permit copy');
  const [notes, setNotes] = useState('Routine scheduled site audit and telecom cabinet access inspection.');
  const [technicianName, setTechnicianName] = useState(currentUser?.name || 'M. Djemil');
  const [technicianPhone, setTechnicianPhone] = useState(currentUser?.phone || '+213 550 12 34 56');

  // Mail settings
  const [mailConfig, setMailConfig] = useState<MailConfig>(() => StorageService.getMailConfig());
  const [recipientEmail, setRecipientEmail] = useState(mailConfig.toGroup);
  const [ccRecipientsEmail, setCcRecipientsEmail] = useState(mailConfig.ccGroup);

  // Synchronize technician name when user changes
  useEffect(() => {
    if (currentUser) {
      setTechnicianName(currentUser.name);
      setTechnicianPhone(currentUser.phone);
    }
  }, [currentUser]);

  // Telemetry
  const [gpsData, setGpsData] = useState<GpsData>(() => TelemetryService.getMockGps(36.754800, 3.056900));
  const [isRefreshingGps, setIsRefreshingGps] = useState(false);
  const [gsmData, setGsmData] = useState<GsmData>(() => TelemetryService.getGsmTelemetry('Mobiserve / Djezzy'));
  const [isRefreshingGsm, setIsRefreshingGsm] = useState(false);

  // Photos
  const [photos, setPhotos] = useState<SitePhoto[]>([]);
  const [isCameraOpen, setIsCameraOpen] = useState(false);
  const [activeCameraTag, setActiveCameraTag] = useState('Site Entrance');

  // Modals & sending state
  const [showEmailPreview, setShowEmailPreview] = useState(false);
  const [isSendingEmail, setIsSendingEmail] = useState(false);
  const [sendResultDialog, setSendResultDialog] = useState<{
    show: boolean;
    success: boolean;
    title: string;
    message: string;
    details?: string;
  }>({
    show: false,
    success: true,
    title: '',
    message: ''
  });

  // Storage data & Supabase site_db
  const [telecomSites, setTelecomSites] = useState<TelecomSite[]>(() => StorageService.getSites());
  const [isSyncingSites, setIsSyncingSites] = useState(false);
  const [supabaseSitesStatus, setSupabaseSitesStatus] = useState<string | null>(null);
  const [selectedSiteForMap, setSelectedSiteForMap] = useState<TelecomSite | null>(null);
  const [reports, setReports] = useState<SiteReport[]>(() => StorageService.getReports());
  const [allUsers, setAllUsers] = useState<UserProfile[]>(() => StorageService.getUsers());
  const [isSyncingQueue, setIsSyncingQueue] = useState(false);
  const [toastMessage, setToastMessage] = useState<string | null>(null);

  const isSupabaseConnected = isSupabaseConfigured();

  const showToast = useCallback((msg: string) => {
    setToastMessage(msg);
    setTimeout(() => {
      setToastMessage((prev) => (prev === msg ? null : prev));
    }, 4500);
  }, []);

  const clearToast = useCallback(() => {
    setToastMessage(null);
  }, []);

  // Fetch site information from site_db table in Supabase
  const fetchSitesFromSupabase = useCallback(async (isManual = false) => {
    setIsSyncingSites(true);
    try {
      const result = await SupabaseSiteService.fetchSitesFromSiteDb();
      if (result.sites && result.sites.length > 0) {
        setTelecomSites(result.sites);
        StorageService.saveSites(result.sites);
        setSupabaseSitesStatus(`Connected: Loaded ${result.sites.length} sites from Supabase site_db table.`);
        if (isManual) {
          showToast(`Synced ${result.sites.length} sites from Supabase site_db`);
        }
      } else {
        const msg = result.error || '0 records returned from site_db';
        setSupabaseSitesStatus(msg);
        if (isManual) {
          showToast(`Supabase site_db: ${msg}`);
        }
      }
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : String(err);
      setSupabaseSitesStatus(`Error: ${msg}`);
      if (isManual) {
        showToast(`Supabase error: ${msg}`);
      }
    } finally {
      setIsSyncingSites(false);
    }
  }, [showToast]);

  // Initial fetch from Supabase site_db table on mount
  useEffect(() => {
    fetchSitesFromSupabase(false);
  }, [fetchSitesFromSupabase]);

  // Compute pending queue count
  const pendingCount = reports.filter(r => r.syncStatus !== 'SYNCED').length;

  // Refresh GPS
  const refreshLocation = useCallback(async () => {
    setIsRefreshingGps(true);
    try {
      const loc = await TelemetryService.getCurrentLocation();
      setGpsData(loc);
      showToast(`GPS Refreshed: ${loc.coordinatesString} (±${loc.accuracy}m)`);
    } catch {
      showToast('Could not fetch GPS location.');
    } finally {
      setIsRefreshingGps(false);
    }
  }, [showToast]);

  // Refresh GSM
  const refreshGsm = useCallback(() => {
    setIsRefreshingGsm(true);
    setTimeout(() => {
      const gsm = TelemetryService.getGsmTelemetry(operator);
      setGsmData(gsm);
      setIsRefreshingGsm(false);
      showToast(`Cell Telemetry Updated: ${gsm.networkType} (CID ${gsm.cellId})`);
    }, 400);
  }, [operator, showToast]);

  // Initial location fetch
  useEffect(() => {
    refreshLocation();
  }, [refreshLocation]);

  // Photo handlers
  const addPhoto = useCallback((photo: SitePhoto) => {
    setPhotos(prev => [photo, ...prev]);
    showToast(`Photo stamped and attached (${photo.tag})`);
  }, [showToast]);

  const removePhoto = useCallback((id: string) => {
    setPhotos(prev => prev.filter(p => p.id !== id));
  }, []);

  const openCamera = useCallback((tag = 'Site Entrance') => {
    setActiveCameraTag(tag);
    setIsCameraOpen(true);
  }, []);

  const closeCamera = useCallback(() => {
    setIsCameraOpen(false);
  }, []);

  // Load site into audit
  const loadSiteIntoAudit = useCallback((site: TelecomSite) => {
    setSiteId(site.siteId);
    setSiteName(site.siteName);
    setOperator(site.operator);
    if (site.accessStatus === 'ACCORDED') {
      setStatus('ACCORDED');
      setRefusalReason('');
    } else if (site.accessStatus === 'REFUSED') {
      setStatus('REFUSED');
      setRefusalReason(site.refusalReason || 'Site access refused');
    }
    setNotes(site.accessInstructions ? `Site Instructions: ${site.accessInstructions}` : '');
    setActiveTab('AUDIT');
    showToast(`Loaded ${site.siteId} into Site Audit`);
  }, [showToast]);

  // Load past report into audit
  const loadReportIntoAudit = useCallback((report: SiteReport) => {
    setSiteId(report.siteId);
    setSiteName(report.siteName);
    setOperator(report.operator);
    setStatus(report.status);
    setRefusalReason(report.refusalReason || '');
    setNotes(report.notes);
    setTechnicianName(report.technicianName);
    setTechnicianPhone(report.technicianPhone);
    setRecipientEmail(report.recipientEmail);
    if (report.ccRecipientsEmail) {
      setCcRecipientsEmail(report.ccRecipientsEmail);
    }
    setPhotos(report.photos);
    setGpsData(report.gps);
    setGsmData(report.gsm);
    setActiveTab('AUDIT');
    showToast(`Loaded past audit report for ${report.siteId}`);
  }, [showToast]);

  // Delete report
  const deleteReport = useCallback((id: string) => {
    StorageService.deleteReport(id);
    setReports(StorageService.getReports());
    showToast('Report deleted from history.');
  }, [showToast]);

  // Send audit report
  const sendAuditReport = async () => {
    setIsSendingEmail(true);
    try {
      const result = await EmailService.sendEmailDispatch({
        siteId,
        siteName,
        operator,
        status,
        refusalReason: status === 'REFUSED' ? refusalReason : undefined,
        notes,
        technicianName,
        technicianPhone,
        recipientEmail,
        ccGroup: ccRecipientsEmail,
        gps: gpsData,
        gsm: gsmData,
        photoCount: photos.length,
        senderEmail: mailConfig.senderEmail
      });

      const newReport: SiteReport = {
        id: `rep_${Date.now()}`,
        siteId,
        siteName,
        operator,
        status,
        refusalReason: status === 'REFUSED' ? refusalReason : undefined,
        technicianName,
        technicianPhone,
        technicianEmail: currentUser?.email || 'technician@mobiserveholding.com',
        recipientEmail,
        ccRecipientsEmail,
        notes,
        gps: gpsData,
        gsm: gsmData,
        photos,
        createdAt: Date.now(),
        syncStatus: result.success ? 'SYNCED' : 'PENDING',
        syncedAt: result.success ? Date.now() : undefined,
        syncErrorMessage: result.success ? undefined : result.message
      };

      StorageService.addReport(newReport);
      setReports(StorageService.getReports());

      // Update matching site status if found
      const matching = telecomSites.find(s => s.siteId.toLowerCase() === siteId.toLowerCase());
      if (matching) {
        const updatedSite: TelecomSite = {
          ...matching,
          accessStatus: status,
          refusalReason: status === 'REFUSED' ? refusalReason : undefined,
          lastAuditTimestamp: Date.now()
        };
        StorageService.updateSite(updatedSite);
        setTelecomSites(StorageService.getSites());
      }

      setSendResultDialog({
        show: true,
        success: result.success,
        title: result.success ? 'Audit Report Dispatched' : 'Queued for Offline Sync',
        message: result.message,
        details: result.details
      });
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Unknown error';
      setSendResultDialog({
        show: true,
        success: false,
        title: 'Transmission Failed',
        message: 'Could not send audit report to Zoho Mail.',
        details: msg
      });
    } finally {
      setIsSendingEmail(false);
    }
  };

  // Save as draft queue
  const saveAsDraftQueue = () => {
    const draftReport: SiteReport = {
      id: `draft_${Date.now()}`,
      siteId,
      siteName,
      operator,
      status,
      refusalReason: status === 'REFUSED' ? refusalReason : undefined,
      technicianName,
      technicianPhone,
      technicianEmail: currentUser?.email || 'technician@mobiserveholding.com',
      recipientEmail,
      ccRecipientsEmail,
      notes,
      gps: gpsData,
      gsm: gsmData,
      photos,
      createdAt: Date.now(),
      syncStatus: 'PENDING',
      syncErrorMessage: 'Saved locally as pending audit record'
    };
    StorageService.addReport(draftReport);
    setReports(StorageService.getReports());
    showToast('Audit report saved to local queue (Pending Sync)');
  };

  // Sync offline queue
  const syncPendingReports = async () => {
    if (!navigator.onLine) {
      showToast('Cannot sync: Device is offline.');
      return;
    }
    setIsSyncingQueue(true);
    await new Promise(r => setTimeout(r, 1000));
    const all = StorageService.getReports().map(r => ({
      ...r,
      syncStatus: 'SYNCED' as const,
      syncedAt: Date.now()
    }));
    StorageService.saveReports(all);
    setReports(all);
    setIsSyncingQueue(false);
    showToast('All pending audit records successfully synced!');
  };

  // Close send result dialog
  const closeSendResultDialog = () => {
    setSendResultDialog(prev => ({ ...prev, show: false }));
  };

  // Auth functions
  const signIn = async (email: string, _password?: string) => {
    const clean = email.trim().toLowerCase();
    const users = StorageService.getUsers();
    let found = users.find(u => u.email.toLowerCase() === clean);

    // Initial root admin check
    const isRootAdmin = clean === 'maamardjemil@gmail.com' || clean === 'etb.djemil@gmail.com' || clean === 'mdjemil@mobiserveholding.com';

    if (!found) {
      if (isRootAdmin) {
        found = {
          id: 'usr_root_admin',
          name: 'M. Djemil',
          email: clean,
          phone: '+213 550 12 34 56',
          role: 'ADMIN',
          department: 'Management / NOC',
          region: 'National',
          isActive: true,
          createdAt: new Date().toISOString()
        };
        StorageService.addUser(found);
        setAllUsers(StorageService.getUsers());
      } else {
        return { success: false, message: 'Account not found. Please register or select a demo user.' };
      }
    }

    if (!found.isActive && !isRootAdmin) {
      setCurrentUser(found);
      setIsPendingActivation(true);
      StorageService.setCurrentUser(found);
      return { success: true, message: 'Account pending administrator activation.' };
    }

    setCurrentUser(found);
    setIsPendingActivation(false);
    StorageService.setCurrentUser(found);
    return { success: true };
  };

  const signUp = async (data: {
    name: string;
    email: string;
    phone: string;
    department: string;
    region: string;
    role: UserRoleCode;
  }) => {
    const clean = data.email.trim().toLowerCase();
    const isRootAdmin = clean === 'maamardjemil@gmail.com' || clean === 'etb.djemil@gmail.com' || clean === 'mdjemil@mobiserveholding.com';
    const newUser: UserProfile = {
      id: `usr_${Date.now()}`,
      name: data.name.trim(),
      email: clean,
      phone: data.phone.trim(),
      role: isRootAdmin ? 'ADMIN' : data.role,
      department: data.department.trim(),
      region: data.region.trim(),
      isActive: isRootAdmin, // Root admin is activated automatically, others await admin approval
      createdAt: new Date().toISOString()
    };

    StorageService.addUser(newUser);
    setAllUsers(StorageService.getUsers());
    setCurrentUser(newUser);
    setIsPendingActivation(!isRootAdmin);
    StorageService.setCurrentUser(newUser);

    if (isRootAdmin) {
      return { success: true, message: 'Root Administrator account activated automatically!' };
    } else {
      return { success: true, message: 'Account registered. Awaiting Administrator activation.' };
    }
  };

  const signOut = () => {
    setCurrentUser(null);
    setIsPendingActivation(false);
    StorageService.setCurrentUser(null);
    showToast('Signed out of session.');
  };

  const switchRole = (role: UserRoleCode) => {
    if (currentUser) {
      const updated = { ...currentUser, role };
      setCurrentUser(updated);
      StorageService.updateUser(updated);
      StorageService.setCurrentUser(updated);
      setAllUsers(StorageService.getUsers());
      showToast(`Switched active role to ${role}`);
    }
  };

  // Admin user directory management
  const pendingUsers = allUsers.filter(u => !u.isActive);

  const approveUser = (id: string) => {
    const user = allUsers.find(u => u.id === id);
    if (user) {
      const updated = { ...user, isActive: true };
      StorageService.updateUser(updated);
      setAllUsers(StorageService.getUsers());
      showToast(`User ${user.name} (${user.email}) activated.`);
    }
  };

  const rejectUser = (id: string) => {
    const updated = allUsers.filter(u => u.id !== id);
    StorageService.saveUsers(updated);
    setAllUsers(updated);
    showToast('User registration removed.');
  };

  const updateUserRole = (id: string, role: UserRoleCode) => {
    const user = allUsers.find(u => u.id === id);
    if (user) {
      const updated = { ...user, role };
      StorageService.updateUser(updated);
      setAllUsers(StorageService.getUsers());
      if (currentUser?.id === id) {
        setCurrentUser(updated);
        StorageService.setCurrentUser(updated);
      }
      showToast(`Updated role for ${user.name} to ${role}`);
    }
  };

  const addUserDirectly = (userData: Omit<UserProfile, 'id' | 'createdAt'>) => {
    const newUser: UserProfile = {
      id: `usr_${Date.now()}`,
      ...userData,
      createdAt: new Date().toISOString()
    };
    StorageService.addUser(newUser);
    setAllUsers(StorageService.getUsers());
    showToast(`Added ${newUser.name} to User Directory`);
  };

  // Add new Telecom site
  const addNewTelecomSite = (siteData: Omit<TelecomSite, 'id'>) => {
    const newSite: TelecomSite = {
      id: `site_${Date.now()}`,
      ...siteData
    };
    StorageService.addSite(newSite);
    setTelecomSites(StorageService.getSites());
    showToast(`Added ${newSite.siteId} (${newSite.siteName}) to Directory`);
  };

  // Admin mail config update
  const updateMailConfig = (partial: Partial<MailConfig>) => {
    const updated = { ...mailConfig, ...partial };
    setMailConfig(updated);
    StorageService.saveMailConfig(updated);
    setRecipientEmail(updated.toGroup);
    setCcRecipientsEmail(updated.ccGroup);
    showToast('Mail routing & Zoho SMTP settings updated.');
  };

  return (
    <AppContext.Provider
      value={{
        currentUser,
        currentRole,
        isPendingActivation,
        signIn,
        signUp,
        signOut,
        switchRole,
        activeTab,
        setActiveTab,
        isOnline,
        pendingCount,
        isSyncingQueue,
        syncPendingReports,
        siteId,
        setSiteId,
        siteName,
        setSiteName,
        operator,
        setOperator,
        status,
        setStatus,
        refusalReason,
        setRefusalReason,
        notes,
        setNotes,
        technicianName,
        setTechnicianName,
        technicianPhone,
        setTechnicianPhone,
        recipientEmail,
        setRecipientEmail,
        ccRecipientsEmail,
        setCcRecipientsEmail,
        gpsData,
        isRefreshingGps,
        refreshLocation,
        gsmData,
        isRefreshingGsm,
        refreshGsm,
        photos,
        addPhoto,
        removePhoto,
        isCameraOpen,
        openCamera,
        closeCamera,
        activeCameraTag,
        setActiveCameraTag,
        showEmailPreview,
        setShowEmailPreview,
        isSendingEmail,
        sendResultDialog,
        closeSendResultDialog,
        sendAuditReport,
        saveAsDraftQueue,
        telecomSites,
        isSyncingSites,
        supabaseSitesStatus,
        supabaseUrl: SUPABASE_URL,
        isSupabaseConnected,
        fetchSitesFromSupabase,
        selectedSiteForMap,
        selectSiteForMap: setSelectedSiteForMap,
        loadSiteIntoAudit,
        addNewTelecomSite,
        reports,
        loadReportIntoAudit,
        deleteReport,
        mailConfig,
        updateMailConfig,
        allUsers,
        pendingUsers,
        approveUser,
        rejectUser,
        updateUserRole,
        addUserDirectly,
        toastMessage,
        showToast,
        clearToast
      }}
    >
      {children}
    </AppContext.Provider>
  );
};

export const useApp = () => {
  const context = useContext(AppContext);
  if (!context) {
    throw new Error('useApp must be used within an AppProvider');
  }
  return context;
};
