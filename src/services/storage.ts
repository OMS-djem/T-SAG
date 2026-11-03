import { TelecomSite, UserProfile, SiteReport, MailConfig } from '../types';

const STORAGE_KEYS = {
  SITES: 'tsag_telecom_sites',
  USERS: 'tsag_users_directory',
  REPORTS: 'tsag_audit_reports',
  MAIL_CONFIG: 'tsag_mail_config',
  CURRENT_USER: 'tsag_current_session_user',
  FORM_DRAFT: 'tsag_form_draft'
};

export const INITIAL_SITES: TelecomSite[] = [
  {
    id: 'site_1',
    siteId: 'MOB-ALG-1044',
    siteName: 'Hydra Telecom Tower',
    operator: 'Mobiserve',
    latitude: 36.754800,
    longitude: 3.056900,
    altitude: 48.0,
    address: 'Rue Djenane El Malik, Hydra, Algiers',
    region: 'Algiers Central',
    siteType: 'Rooftop Tower',
    accessStatus: 'ACCORDED',
    accessInstructions: 'Security badge & ID required at reception gate',
    contactPerson: 'Farid Touati (Site Manager)',
    contactPhone: '+213 550 99 88 77'
  },
  {
    id: 'site_2',
    siteId: 'DJZ-ALG-0218',
    siteName: 'Bab Ezzouar Cyberpark Mast',
    operator: 'Djezzy',
    latitude: 36.721400,
    longitude: 3.182500,
    altitude: 32.0,
    address: 'Cyberpark Sidi Abdellah / Bab Ezzouar',
    region: 'Algiers East',
    siteType: 'Greenfield Mast',
    accessStatus: 'PENDING',
    accessInstructions: 'Work permit signed by NOC required for climbing',
    contactPerson: 'Nassim Brahimi',
    contactPhone: '+213 551 11 22 33'
  },
  {
    id: 'site_3',
    siteId: 'MOB-ALG-0512',
    siteName: 'Kouba Plateau Central',
    operator: 'Mobilis',
    latitude: 36.732800,
    longitude: 3.078900,
    altitude: 115.0,
    address: 'Avenue des Frères Abdeslami, Kouba, Algiers',
    region: 'Algiers South',
    siteType: 'Monopole',
    accessStatus: 'ACCORDED',
    accessInstructions: 'Security gate combination code: 4492',
    contactPerson: 'Karim Cherif',
    contactPhone: '+213 552 33 44 55'
  },
  {
    id: 'site_4',
    siteId: 'OOR-ALG-0891',
    siteName: 'El Biar Relay Station',
    operator: 'Ooredoo',
    latitude: 36.768500,
    longitude: 3.032100,
    altitude: 160.0,
    address: 'Chemin Mackley, El Biar, Algiers',
    region: 'Algiers West',
    siteType: 'Rooftop Tower',
    accessStatus: 'REFUSED',
    refusalReason: 'Building syndicate refused roof access without 48h advance notice',
    accessInstructions: 'Contact building manager M. Zinedine prior to arrival',
    contactPerson: 'Zinedine Kaci',
    contactPhone: '+213 553 44 55 66'
  },
  {
    id: 'site_5',
    siteId: 'MOB-ALG-1205',
    siteName: 'Bir Mourad Rais Monopole',
    operator: 'Mobiserve',
    latitude: 36.741000,
    longitude: 3.051000,
    altitude: 78.0,
    address: 'Route de Blida, Bir Mourad Rais, Algiers',
    region: 'Algiers Central',
    siteType: 'Monopole',
    accessStatus: 'ACCORDED',
    accessInstructions: 'Security guard M. Amine on duty 24/7',
    contactPerson: 'Amine Djelloul',
    contactPhone: '+213 554 55 66 77'
  },
  {
    id: 'site_6',
    siteId: 'DJZ-ALG-0734',
    siteName: 'Sidi Yahia Commercial Microcell',
    operator: 'Djezzy',
    latitude: 36.758200,
    longitude: 3.039500,
    altitude: 92.0,
    address: 'Boulevard Sidi Yahia, Hydra, Algiers',
    region: 'Algiers Central',
    siteType: 'Indoor Microcell',
    accessStatus: 'PENDING',
    accessInstructions: 'Mall engineering basement key required',
    contactPerson: 'Sofiane Benali',
    contactPhone: '+213 555 66 77 88'
  },
  {
    id: 'site_7',
    siteId: 'MOB-OR-2104',
    siteName: 'Oran Es-Senia Regional Hub',
    operator: 'Mobilis',
    latitude: 35.631100,
    longitude: -0.621000,
    altitude: 90.0,
    address: 'Zone Industrielle Es-Senia, Oran',
    region: 'Oran',
    siteType: 'Greenfield Tower',
    accessStatus: 'ACCORDED',
    accessInstructions: 'Escort by military liaison required on perimeter',
    contactPerson: 'Rabah Mansouri',
    contactPhone: '+213 556 77 88 99'
  },
  {
    id: 'site_8',
    siteId: 'OOR-CST-3309',
    siteName: 'Constantine Zouaghi Mast',
    operator: 'Ooredoo',
    latitude: 36.321000,
    longitude: 6.615000,
    altitude: 650.0,
    address: 'Plateau Zouaghi Slimane, Constantine',
    region: 'Constantine',
    siteType: 'Greenfield Mast',
    accessStatus: 'PENDING',
    accessInstructions: 'NOC work authorization code #891',
    contactPerson: 'Tariq Merabet',
    contactPhone: '+213 557 88 99 00'
  }
];

export const INITIAL_USERS: UserProfile[] = [
  {
    id: 'usr_root_admin',
    name: 'M. Djemil',
    email: 'mdjemil@mobiserveholding.com',
    phone: '+213 550 12 34 56',
    role: 'ADMIN',
    department: 'Management / NOC',
    region: 'National',
    isActive: true,
    createdAt: new Date().toISOString()
  },
  {
    id: 'usr_tech_1',
    name: 'Karim Benali',
    email: 'kbenali@mobiserveholding.com',
    phone: '+213 551 22 33 44',
    role: 'TECHNICIAN',
    department: 'Field Services',
    region: 'Algiers North',
    isActive: true,
    createdAt: new Date().toISOString()
  },
  {
    id: 'usr_om_1',
    name: 'Samir Hadj',
    email: 'shadj@mobiserveholding.com',
    phone: '+213 552 44 55 66',
    role: 'OM',
    department: 'Operations & Maintenance',
    region: 'Central',
    isActive: true,
    createdAt: new Date().toISOString()
  },
  {
    id: 'usr_sup_1',
    name: 'Amine Mansour',
    email: 'amansour@mobiserveholding.com',
    phone: '+213 553 66 77 88',
    role: 'SUPERVISION',
    department: 'Regional Supervision',
    region: 'Oran & West',
    isActive: true,
    createdAt: new Date().toISOString()
  },
  {
    id: 'usr_disp_1',
    name: 'Yacine Cherif',
    email: 'ycherif@mobiserveholding.com',
    phone: '+213 554 88 99 00',
    role: 'DISPATCHER',
    department: 'NOC Work Orders',
    region: 'National',
    isActive: true,
    createdAt: new Date().toISOString()
  },
  {
    id: 'usr_acq_1',
    name: 'Nadia Belkacem',
    email: 'nbelkacem@mobiserveholding.com',
    phone: '+213 555 11 22 33',
    role: 'ACQUISITION',
    department: 'Site Acquisition & Legal',
    region: 'National',
    isActive: true,
    createdAt: new Date().toISOString()
  }
];

export const DEFAULT_MAIL_CONFIG: MailConfig = {
  senderEmail: 'om@mobiserveholding.com',
  senderName: 'Mobiserve Field Operations NOC',
  toGroup: 'mdjemil@mobiserveholding.com',
  ccGroup: 'supervision@mobiserveholding.com, om@mobiserveholding.com',
  host: 'smtppro.zoho.com',
  port: 465,
  password: '••••••••••••'
};

export const INITIAL_REPORTS: SiteReport[] = [
  {
    id: 'rep_1',
    siteId: 'MOB-ALG-1044',
    siteName: 'Hydra Telecom Tower',
    operator: 'Mobiserve / Djezzy',
    status: 'ACCORDED',
    technicianName: 'M. Djemil',
    technicianPhone: '+213 550 12 34 56',
    technicianEmail: 'mdjemil@mobiserveholding.com',
    recipientEmail: 'mdjemil@mobiserveholding.com',
    ccRecipientsEmail: 'supervision@mobiserveholding.com',
    notes: 'Scheduled monthly preventive audit. Battery bank and rectifiers checked, air conditioning filters cleaned. Access granted by building guard with badge.',
    gps: {
      latitude: 36.754800,
      longitude: 3.056900,
      altitude: 48.0,
      accuracy: 3.5,
      address: 'Rue Djenane El Malik, Hydra, Algiers',
      timestamp: Date.now() - 86400000 * 2,
      coordinatesString: '36.754800, 3.056900',
      mapsUrl: 'https://maps.google.com/?q=36.754800,3.056900'
    },
    gsm: {
      operatorName: 'Djezzy GSM',
      networkType: 'LTE / 4G+',
      cellId: '4288194',
      lacTac: '3044',
      mcc: '603',
      mnc: '02',
      signalDbm: -78,
      signalLevel: 'Excellent'
    },
    photos: [],
    createdAt: Date.now() - 86400000 * 2,
    syncStatus: 'SYNCED',
    syncedAt: Date.now() - 86400000 * 2
  },
  {
    id: 'rep_2',
    siteId: 'OOR-ALG-0891',
    siteName: 'El Biar Relay Station',
    operator: 'Ooredoo',
    status: 'REFUSED',
    refusalReason: 'Building syndicate refused roof access without 48h advance notice',
    technicianName: 'Karim Benali',
    technicianPhone: '+213 551 22 33 44',
    technicianEmail: 'kbenali@mobiserveholding.com',
    recipientEmail: 'mdjemil@mobiserveholding.com',
    notes: 'Building trustee M. Kaci stated that access to roof terrace was locked following waterproofing repairs. Requires written NOC permit.',
    gps: {
      latitude: 36.768500,
      longitude: 3.032100,
      altitude: 160.0,
      accuracy: 4.8,
      address: 'Chemin Mackley, El Biar, Algiers',
      timestamp: Date.now() - 86400000 * 5,
      coordinatesString: '36.768500, 3.032100',
      mapsUrl: 'https://maps.google.com/?q=36.768500,3.032100'
    },
    gsm: {
      operatorName: 'Ooredoo DZ',
      networkType: 'LTE / 4G',
      cellId: '1899023',
      lacTac: '4012',
      mcc: '603',
      mnc: '03',
      signalDbm: -89,
      signalLevel: 'Good'
    },
    photos: [],
    createdAt: Date.now() - 86400000 * 5,
    syncStatus: 'SYNCED',
    syncedAt: Date.now() - 86400000 * 5
  },
  {
    id: 'rep_3',
    siteId: 'DJZ-ALG-0218',
    siteName: 'Bab Ezzouar Cyberpark Mast',
    operator: 'Djezzy',
    status: 'ACCORDED',
    technicianName: 'Samir Hadj',
    technicianPhone: '+213 552 44 55 66',
    technicianEmail: 'shadj@mobiserveholding.com',
    recipientEmail: 'mdjemil@mobiserveholding.com',
    notes: 'Access permit verified at security entrance. Microwave link alignment inspection completed without incidents.',
    gps: {
      latitude: 36.721400,
      longitude: 3.182500,
      altitude: 32.0,
      accuracy: 2.8,
      address: 'Cyberpark Sidi Abdellah / Bab Ezzouar',
      timestamp: Date.now() - 86400000 * 9,
      coordinatesString: '36.721400, 3.182500',
      mapsUrl: 'https://maps.google.com/?q=36.721400,3.182500'
    },
    gsm: {
      operatorName: 'Mobilis Telecom',
      networkType: '5G NR',
      cellId: '7611094',
      lacTac: '1099',
      mcc: '603',
      mnc: '01',
      signalDbm: -72,
      signalLevel: 'Excellent'
    },
    photos: [],
    createdAt: Date.now() - 86400000 * 9,
    syncStatus: 'SYNCED',
    syncedAt: Date.now() - 86400000 * 9
  }
];

export const StorageService = {
  getSites(): TelecomSite[] {
    try {
      const data = localStorage.getItem(STORAGE_KEYS.SITES);
      if (!data) {
        localStorage.setItem(STORAGE_KEYS.SITES, JSON.stringify(INITIAL_SITES));
        return INITIAL_SITES;
      }
      return JSON.parse(data);
    } catch {
      return INITIAL_SITES;
    }
  },

  saveSites(sites: TelecomSite[]) {
    localStorage.setItem(STORAGE_KEYS.SITES, JSON.stringify(sites));
  },

  addSite(site: TelecomSite) {
    const sites = this.getSites();
    sites.unshift(site);
    this.saveSites(sites);
  },

  updateSite(updated: TelecomSite) {
    const sites = this.getSites().map(s => s.id === updated.id ? updated : s);
    this.saveSites(sites);
  },

  getUsers(): UserProfile[] {
    try {
      const data = localStorage.getItem(STORAGE_KEYS.USERS);
      if (!data) {
        localStorage.setItem(STORAGE_KEYS.USERS, JSON.stringify(INITIAL_USERS));
        return INITIAL_USERS;
      }
      return JSON.parse(data);
    } catch {
      return INITIAL_USERS;
    }
  },

  saveUsers(users: UserProfile[]) {
    localStorage.setItem(STORAGE_KEYS.USERS, JSON.stringify(users));
  },

  addUser(user: UserProfile) {
    const users = this.getUsers();
    users.unshift(user);
    this.saveUsers(users);
  },

  updateUser(updated: UserProfile) {
    const users = this.getUsers().map(u => u.id === updated.id ? updated : u);
    this.saveUsers(users);
  },

  getReports(): SiteReport[] {
    try {
      const data = localStorage.getItem(STORAGE_KEYS.REPORTS);
      if (!data) {
        localStorage.setItem(STORAGE_KEYS.REPORTS, JSON.stringify(INITIAL_REPORTS));
        return INITIAL_REPORTS;
      }
      return JSON.parse(data);
    } catch {
      return INITIAL_REPORTS;
    }
  },

  saveReports(reports: SiteReport[]) {
    localStorage.setItem(STORAGE_KEYS.REPORTS, JSON.stringify(reports));
  },

  addReport(report: SiteReport) {
    const reports = this.getReports();
    reports.unshift(report);
    this.saveReports(reports);
  },

  deleteReport(id: string) {
    const reports = this.getReports().filter(r => r.id !== id);
    this.saveReports(reports);
  },

  getMailConfig(): MailConfig {
    try {
      const data = localStorage.getItem(STORAGE_KEYS.MAIL_CONFIG);
      if (!data) {
        localStorage.setItem(STORAGE_KEYS.MAIL_CONFIG, JSON.stringify(DEFAULT_MAIL_CONFIG));
        return DEFAULT_MAIL_CONFIG;
      }
      return { ...DEFAULT_MAIL_CONFIG, ...JSON.parse(data) };
    } catch {
      return DEFAULT_MAIL_CONFIG;
    }
  },

  saveMailConfig(config: MailConfig) {
    localStorage.setItem(STORAGE_KEYS.MAIL_CONFIG, JSON.stringify(config));
  },

  getCurrentUser(): UserProfile | null {
    try {
      const data = localStorage.getItem(STORAGE_KEYS.CURRENT_USER);
      if (data) return JSON.parse(data);
      // Default to root admin if not set
      return INITIAL_USERS[0];
    } catch {
      return INITIAL_USERS[0];
    }
  },

  setCurrentUser(user: UserProfile | null) {
    if (user) {
      localStorage.setItem(STORAGE_KEYS.CURRENT_USER, JSON.stringify(user));
    } else {
      localStorage.removeItem(STORAGE_KEYS.CURRENT_USER);
    }
  }
};
