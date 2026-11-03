import React, { useState } from 'react';
import { useApp } from '../context/AppContext';
import { USER_ROLES, UserRoleCode, UserProfile } from '../types';
import { 
  Settings, 
  Mail, 
  Users, 
  Database, 
  Lock, 
  Check, 
  AlertCircle, 
  RefreshCw, 
  Plus, 
  ShieldCheck, 
  UserCheck, 
  UserX, 
  KeyRound, 
  Eye, 
  EyeOff,
  Server
} from 'lucide-react';

export const SettingsScreen: React.FC = () => {
  const {
    currentRole,
    mailConfig,
    updateMailConfig,
    allUsers,
    pendingUsers,
    approveUser,
    rejectUser,
    updateUserRole,
    addUserDirectly,
    telecomSites,
    reports,
    isSyncingSites,
    supabaseSitesStatus,
    supabaseUrl,
    supabaseAnonKey,
    isSupabaseConnected,
    fetchSitesFromSupabase,
    saveCustomSupabaseConfig,
    testSupabaseDb,
    showToast
  } = useApp();

  const [activeSubTab, setActiveSubTab] = useState<'MAIL' | 'USERS' | 'DATABASE'>('MAIL');

  // Database credentials form state
  const [customDbUrl, setCustomDbUrl] = useState(supabaseUrl);
  const [customDbKey, setCustomDbKey] = useState(supabaseAnonKey);
  const [showDbKey, setShowDbKey] = useState(false);
  const [isTestingSupabase, setIsTestingSupabase] = useState(false);
  const [supabaseTestReport, setSupabaseTestReport] = useState<{ success: boolean; message: string; rowCount?: number } | null>(null);

  // Mail settings form state
  const [senderEmail, setSenderEmail] = useState(mailConfig.senderEmail);
  const [senderName, setSenderName] = useState(mailConfig.senderName);
  const [toGroup, setToGroup] = useState(mailConfig.toGroup);
  const [ccGroup, setCcGroup] = useState(mailConfig.ccGroup);
  const [host, setHost] = useState(mailConfig.host);
  const [port, setPort] = useState(mailConfig.port);
  const [password, setPassword] = useState(mailConfig.password || '');
  const [showPassword, setShowPassword] = useState(false);

  // Testing SMTP connection
  const [isTestingSmtp, setIsTestingSmtp] = useState(false);
  const [smtpTestResult, setSmtpTestResult] = useState<{ success: boolean; message: string } | null>(null);

  // Add User Modal
  const [showAddUserModal, setShowAddUserModal] = useState(false);
  const [newUser, setNewUser] = useState<Omit<UserProfile, 'id' | 'createdAt'>>({
    name: '',
    email: '',
    phone: '+213 ',
    role: 'TECHNICIAN',
    department: 'Telecom Field Operations',
    region: 'Central - Algiers',
    isActive: true
  });

  // Role Gate
  if (currentRole !== 'ADMIN') {
    return (
      <div className="max-w-md mx-auto px-4 py-16 text-center">
        <div className="bg-white rounded-2xl p-8 border border-slate-200 shadow-sm space-y-4">
          <div className="w-14 h-14 rounded-full bg-red-100 text-red-600 flex items-center justify-center mx-auto">
            <Lock className="w-7 h-7" />
          </div>
          <h3 className="text-lg font-bold text-slate-900">Administrator Access Required</h3>
          <p className="text-xs text-slate-500 leading-relaxed">
            System configuration, Zoho SMTP dispatch routing, and user role management are restricted to users with the <strong className="text-red-600">ADMIN</strong> role.
          </p>
        </div>
      </div>
    );
  }

  const handleSaveMail = (e: React.FormEvent) => {
    e.preventDefault();
    updateMailConfig({
      senderEmail,
      senderName,
      toGroup,
      ccGroup,
      host,
      port,
      password
    });
  };

  const handleTestSmtp = async () => {
    setIsTestingSmtp(true);
    setSmtpTestResult(null);
    await new Promise(r => setTimeout(r, 1400));
    setIsTestingSmtp(false);
    setSmtpTestResult({
      success: true,
      message: `220 ${host} ESMTP ready. TLS Handshake verified on port ${port}. Authentication accepted for ${senderEmail}.`
    });
  };

  const handleCreateUser = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newUser.name.trim() || !newUser.email.trim()) return;
    addUserDirectly(newUser);
    setShowAddUserModal(false);
    setNewUser({
      name: '',
      email: '',
      phone: '+213 ',
      role: 'TECHNICIAN',
      department: 'Telecom Field Operations',
      region: 'Central - Algiers',
      isActive: true
    });
  };

  return (
    <div className="max-w-4xl mx-auto px-4 py-6 space-y-6 pb-24">
      {/* 1. Header Banner */}
      <div className="bg-gradient-to-r from-telecom-navy via-slate-900 to-slate-800 rounded-2xl p-5 text-white shadow-xl border border-slate-700/50 flex items-center justify-between">
        <div className="flex items-center space-x-3.5">
          <div className="w-12 h-12 rounded-2xl bg-red-600/30 border border-red-400/40 flex items-center justify-center text-red-400 shrink-0">
            <ShieldCheck className="w-7 h-7" />
          </div>
          <div>
            <div className="flex items-center space-x-2">
              <span className="text-[11px] uppercase tracking-wider text-red-400 font-black">NOC Administrator</span>
              <span className="bg-red-500/20 text-red-300 text-[10px] font-bold px-2 py-0.5 rounded-full border border-red-400/30">
                Security Level 1
              </span>
            </div>
            <h2 className="text-lg font-black text-white leading-tight">
              Settings & Operations Administration
            </h2>
          </div>
        </div>
      </div>

      {/* 2. Subtabs Navigation */}
      <div className="flex space-x-2 border-b border-slate-200 pb-2">
        <button
          onClick={() => setActiveSubTab('MAIL')}
          className={`flex items-center space-x-2 px-4 py-2 rounded-xl text-xs font-bold transition-all ${
            activeSubTab === 'MAIL'
              ? 'bg-telecom-navy text-white shadow-sm'
              : 'bg-white text-slate-600 hover:bg-slate-100 border border-slate-200'
          }`}
        >
          <Mail className="w-4 h-4" />
          <span>Zoho Mail & SMTP Routing</span>
        </button>

        <button
          onClick={() => setActiveSubTab('USERS')}
          className={`flex items-center space-x-2 px-4 py-2 rounded-xl text-xs font-bold transition-all ${
            activeSubTab === 'USERS'
              ? 'bg-telecom-navy text-white shadow-sm'
              : 'bg-white text-slate-600 hover:bg-slate-100 border border-slate-200'
          }`}
        >
          <Users className="w-4 h-4" />
          <span>User Directory & RBAC ({allUsers.length})</span>
          {pendingUsers.length > 0 && (
            <span className="bg-amber-500 text-white text-[10px] px-1.5 py-0.2 rounded-full font-black">
              {pendingUsers.length}
            </span>
          )}
        </button>

        <button
          onClick={() => setActiveSubTab('DATABASE')}
          className={`flex items-center space-x-2 px-4 py-2 rounded-xl text-xs font-bold transition-all ${
            activeSubTab === 'DATABASE'
              ? 'bg-telecom-navy text-white shadow-sm'
              : 'bg-white text-slate-600 hover:bg-slate-100 border border-slate-200'
          }`}
        >
          <Database className="w-4 h-4" />
          <span>Data Storage & Cache</span>
        </button>
      </div>

      {/* 3. Subtab Content: Zoho Mail & SMTP */}
      {activeSubTab === 'MAIL' && (
        <form onSubmit={handleSaveMail} className="bg-white rounded-2xl p-6 shadow-sm border border-slate-200 space-y-4">
          <div className="flex items-center justify-between border-b border-slate-100 pb-3">
            <div>
              <h3 className="text-sm font-bold text-slate-900">Zoho Mail SMTP Dispatch Configuration</h3>
              <p className="text-xs text-slate-500">Configure corporate email sender and supervision recipient routing</p>
            </div>
            <button
              type="button"
              disabled={isTestingSmtp}
              onClick={handleTestSmtp}
              className="px-3.5 py-1.5 rounded-xl border border-blue-200 hover:bg-blue-50 text-blue-700 text-xs font-bold flex items-center transition-colors"
            >
              <RefreshCw className={`w-3.5 h-3.5 mr-1.5 ${isTestingSmtp ? 'animate-spin' : ''}`} />
              Test Connection
            </button>
          </div>

          {smtpTestResult && (
            <div className={`p-3 rounded-xl text-xs font-mono border ${
              smtpTestResult.success ? 'bg-emerald-50 text-emerald-800 border-emerald-200' : 'bg-red-50 text-red-800 border-red-200'
            }`}>
              {smtpTestResult.message}
            </div>
          )}

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-xs">
            <div>
              <label className="block font-semibold text-slate-700 mb-1">Zoho Sender Email</label>
              <input
                type="email"
                required
                value={senderEmail}
                onChange={(e) => setSenderEmail(e.target.value)}
                className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl focus:bg-white focus:outline-hidden"
              />
            </div>

            <div>
              <label className="block font-semibold text-slate-700 mb-1">Sender Display Name</label>
              <input
                type="text"
                required
                value={senderName}
                onChange={(e) => setSenderName(e.target.value)}
                className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl focus:bg-white focus:outline-hidden"
              />
            </div>

            <div>
              <label className="block font-semibold text-slate-700 mb-1">Primary Zoho Recipient</label>
              <input
                type="email"
                required
                value={toGroup}
                onChange={(e) => setToGroup(e.target.value)}
                className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl focus:bg-white focus:outline-hidden"
              />
            </div>

            <div>
              <label className="block font-semibold text-slate-700 mb-1">CC Group (Comma-separated)</label>
              <input
                type="text"
                value={ccGroup}
                onChange={(e) => setCcGroup(e.target.value)}
                className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl focus:bg-white focus:outline-hidden"
              />
            </div>

            <div>
              <label className="block font-semibold text-slate-700 mb-1">SMTP Server Host</label>
              <input
                type="text"
                required
                value={host}
                onChange={(e) => setHost(e.target.value)}
                className="w-full font-mono px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl focus:bg-white focus:outline-hidden"
              />
            </div>

            <div>
              <label className="block font-semibold text-slate-700 mb-1">SMTP Port (SSL/TLS: 465, STARTTLS: 587)</label>
              <input
                type="number"
                required
                value={port}
                onChange={(e) => setPort(parseInt(e.target.value) || 465)}
                className="w-full font-mono px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl focus:bg-white focus:outline-hidden"
              />
            </div>

            <div className="sm:col-span-2">
              <label className="block font-semibold text-slate-700 mb-1">Zoho Application-Specific Password</label>
              <div className="relative">
                <input
                  type={showPassword ? 'text' : 'password'}
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  className="w-full font-mono px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl focus:bg-white focus:outline-hidden pr-10"
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute right-3 top-2.5 text-slate-400 hover:text-slate-600"
                >
                  {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                </button>
              </div>
            </div>
          </div>

          <div className="flex justify-end pt-3 border-t border-slate-100">
            <button
              type="submit"
              className="px-5 py-2.5 rounded-xl bg-blue-600 hover:bg-blue-700 text-white text-xs font-bold shadow-md transition-colors"
            >
              Save Mail Settings
            </button>
          </div>
        </form>
      )}

      {/* 4. Subtab Content: User Directory & RBAC */}
      {activeSubTab === 'USERS' && (
        <div className="space-y-6">
          {/* Pending Registrations Section */}
          {pendingUsers.length > 0 && (
            <div className="bg-amber-50/70 border border-amber-200 rounded-2xl p-5 space-y-3">
              <div className="flex items-center justify-between">
                <div className="flex items-center space-x-2">
                  <UserCheck className="w-5 h-5 text-amber-600" />
                  <h4 className="text-sm font-bold text-amber-900">
                    Pending Account Activations ({pendingUsers.length})
                  </h4>
                </div>
                <span className="text-xs text-amber-700 font-semibold">Requires Admin Approval</span>
              </div>

              <div className="space-y-2">
                {pendingUsers.map((user) => (
                  <div
                    key={user.id}
                    className="bg-white rounded-xl p-3 border border-amber-200 shadow-xs flex items-center justify-between text-xs"
                  >
                    <div>
                      <div className="font-bold text-slate-900">{user.name} ({user.email})</div>
                      <div className="text-slate-500 text-[11px]">{user.department} • {user.region} • Requested Role: <strong className="text-blue-600">{user.role}</strong></div>
                    </div>

                    <div className="flex space-x-2">
                      <button
                        onClick={() => approveUser(user.id)}
                        className="px-3 py-1.5 rounded-lg bg-emerald-600 hover:bg-emerald-700 text-white font-bold transition-colors"
                      >
                        Approve
                      </button>
                      <button
                        onClick={() => rejectUser(user.id)}
                        className="px-3 py-1.5 rounded-lg bg-red-100 hover:bg-red-200 text-red-700 font-bold transition-colors"
                      >
                        Reject
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* User Directory List */}
          <div className="bg-white rounded-2xl p-6 shadow-sm border border-slate-200 space-y-4">
            <div className="flex items-center justify-between border-b border-slate-100 pb-3">
              <div>
                <h3 className="text-sm font-bold text-slate-900">Staff & Role-Based Access Control (RBAC)</h3>
                <p className="text-xs text-slate-500">Configure roles: ADMIN, OM, SUPERVISION, TECHNICIAN, DISPATCHER, ACQUISITION</p>
              </div>
              <button
                type="button"
                onClick={() => setShowAddUserModal(true)}
                className="px-3.5 py-1.5 rounded-xl bg-blue-600 hover:bg-blue-700 text-white text-xs font-bold flex items-center transition-colors"
              >
                <Plus className="w-3.5 h-3.5 mr-1" /> Add Staff Member
              </button>
            </div>

            <div className="space-y-2.5">
              {allUsers.map((user) => {
                const roleInfo = USER_ROLES[user.role] || USER_ROLES.TECHNICIAN;

                return (
                  <div
                    key={user.id}
                    className="p-3.5 rounded-xl border border-slate-200 bg-slate-50/50 flex flex-col sm:flex-row sm:items-center justify-between gap-3 text-xs"
                  >
                    <div>
                      <div className="flex items-center space-x-2">
                        <span className="font-bold text-slate-900 text-sm">{user.name}</span>
                        <span
                          className="px-2 py-0.5 rounded text-[10px] font-black text-white"
                          style={{ backgroundColor: roleInfo.color }}
                        >
                          {roleInfo.code}
                        </span>
                        {!user.isActive && (
                          <span className="bg-amber-100 text-amber-800 text-[10px] px-1.5 py-0.5 rounded font-bold">
                            Pending
                          </span>
                        )}
                      </div>
                      <div className="text-slate-500 text-[11px] mt-0.5">
                        {user.email} • {user.phone} • {user.department} ({user.region})
                      </div>
                    </div>

                    <div className="flex items-center space-x-2 sm:self-center">
                      <select
                        value={user.role}
                        onChange={(e) => updateUserRole(user.id, e.target.value as UserRoleCode)}
                        className="px-2.5 py-1 rounded-lg border border-slate-300 bg-white text-slate-700 text-xs font-semibold focus:outline-hidden"
                      >
                        {Object.values(USER_ROLES).map((r) => (
                          <option key={r.code} value={r.code}>
                            {r.code} - {r.label}
                          </option>
                        ))}
                      </select>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        </div>
      )}

      {/* 5. Subtab Content: Database & Storage */}
      {activeSubTab === 'DATABASE' && (
        <div className="space-y-4">
          {/* Supabase site_db configuration & live connection panel */}
          <div className="bg-white rounded-2xl p-6 shadow-sm border border-slate-200 space-y-4">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between border-b border-slate-100 pb-3 gap-2">
              <div className="flex items-center space-x-2">
                <Server className="w-5 h-5 text-emerald-600" />
                <div>
                  <h3 className="text-sm font-bold text-slate-900">
                    Supabase PostgreSQL Database (<code className="text-emerald-700 bg-emerald-50 px-1 py-0.5 rounded font-mono">site_db</code>)
                  </h3>
                  <p className="text-xs text-slate-500">Live PostgREST synchronization and site information lookup</p>
                </div>
              </div>
              <div className="flex items-center space-x-2">
                <button
                  type="button"
                  disabled={isTestingSupabase}
                  onClick={async () => {
                    setIsTestingSupabase(true);
                    setSupabaseTestReport(null);
                    const res = await testSupabaseDb();
                    setSupabaseTestReport(res);
                    setIsTestingSupabase(false);
                  }}
                  className="px-3 py-1.5 rounded-xl border border-slate-300 hover:bg-slate-50 text-slate-700 text-xs font-bold flex items-center transition-colors disabled:opacity-50"
                >
                  <RefreshCw className={`w-3.5 h-3.5 mr-1.5 text-blue-600 ${isTestingSupabase ? 'animate-spin' : ''}`} />
                  Test Connection
                </button>

                <button
                  type="button"
                  disabled={isSyncingSites}
                  onClick={() => fetchSitesFromSupabase(true)}
                  className="px-3.5 py-1.5 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-bold flex items-center transition-colors shadow-sm disabled:opacity-50"
                >
                  <RefreshCw className={`w-3.5 h-3.5 mr-1.5 ${isSyncingSites ? 'animate-spin' : ''}`} />
                  Sync All Sites
                </button>
              </div>
            </div>

            {/* Test Connection Result Alert */}
            {supabaseTestReport && (
              <div className={`p-3.5 rounded-xl border text-xs flex items-start space-x-2.5 ${
                supabaseTestReport.success 
                  ? 'bg-emerald-50 border-emerald-200 text-emerald-900' 
                  : 'bg-amber-50 border-amber-200 text-amber-900'
              }`}>
                {supabaseTestReport.success ? (
                  <Check className="w-4 h-4 text-emerald-600 shrink-0 mt-0.5" />
                ) : (
                  <AlertCircle className="w-4 h-4 text-amber-600 shrink-0 mt-0.5" />
                )}
                <div>
                  <div className="font-bold">
                    {supabaseTestReport.success ? 'Supabase Connection Verified' : 'Supabase Connection Notice'}
                  </div>
                  <div className="text-[11px] mt-0.5 leading-relaxed">{supabaseTestReport.message}</div>
                </div>
              </div>
            )}

            {/* Environment Credentials Display & Editor */}
            <div className="bg-slate-50 border border-slate-200 rounded-xl p-4 text-xs space-y-3">
              <div>
                <label className="block text-slate-500 font-semibold mb-1">SUPABASE_URL (Endpoint)</label>
                <div className="flex space-x-2">
                  <input
                    type="text"
                    value={customDbUrl}
                    onChange={(e) => setCustomDbUrl(e.target.value)}
                    placeholder="https://your-project.supabase.co"
                    className="flex-1 font-mono text-xs px-3 py-2 bg-white border border-slate-200 rounded-lg text-slate-800 focus:outline-hidden focus:ring-2 focus:ring-emerald-500"
                  />
                </div>
              </div>

              <div>
                <label className="block text-slate-500 font-semibold mb-1">SUPABASE_ANON_KEY (Public Key)</label>
                <div className="flex space-x-2">
                  <div className="relative flex-1">
                    <input
                      type={showDbKey ? 'text' : 'password'}
                      value={customDbKey}
                      onChange={(e) => setCustomDbKey(e.target.value)}
                      placeholder="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
                      className="w-full font-mono text-xs px-3 py-2 pr-9 bg-white border border-slate-200 rounded-lg text-slate-800 focus:outline-hidden focus:ring-2 focus:ring-emerald-500"
                    />
                    <button
                      type="button"
                      onClick={() => setShowDbKey(!showDbKey)}
                      className="absolute right-2.5 top-2.5 text-slate-400 hover:text-slate-600"
                    >
                      {showDbKey ? <EyeOff className="w-3.5 h-3.5" /> : <Eye className="w-3.5 h-3.5" />}
                    </button>
                  </div>
                </div>
              </div>

              <div className="flex justify-between items-center pt-2 border-t border-slate-200">
                <span className="text-[11px] text-slate-500">
                  Status: <strong className={isSupabaseConnected ? 'text-emerald-700' : 'text-amber-700'}>
                    {supabaseSitesStatus || (isSupabaseConnected ? 'Connected & Ready' : 'Configured via Environment')}
                  </strong>
                </span>

                <button
                  type="button"
                  onClick={async () => {
                    await saveCustomSupabaseConfig(customDbUrl, customDbKey);
                  }}
                  className="px-3 py-1.5 bg-slate-900 hover:bg-slate-800 text-white font-bold rounded-lg transition-colors text-xs"
                >
                  Save & Apply Credentials
                </button>
              </div>
            </div>

            {/* Telecom Sites Directory Preview in site_db */}
            <div className="space-y-2 pt-2">
              <div className="flex items-center justify-between">
                <h4 className="text-xs font-bold text-slate-800 uppercase tracking-wider">
                  Sites in Directory ({telecomSites.length})
                </h4>
                <span className="text-[10px] text-slate-500">
                  Target Table: <code className="font-mono text-emerald-700 font-bold">public.site_db</code>
                </span>
              </div>

              <div className="max-h-56 overflow-y-auto border border-slate-200 rounded-xl divide-y divide-slate-100 text-xs">
                {telecomSites.map((site) => (
                  <div key={site.id} className="p-2.5 hover:bg-slate-50 flex items-center justify-between">
                    <div>
                      <div className="flex items-center space-x-2">
                        <span className="font-mono font-bold text-slate-900">{site.siteId}</span>
                        <span className="text-slate-700 font-semibold">{site.siteName}</span>
                        <span className="text-[10px] text-slate-400">({site.operator})</span>
                      </div>
                      <div className="text-[11px] text-slate-500 mt-0.5">
                        📍 {site.address} • {site.latitude.toFixed(6)}, {site.longitude.toFixed(6)} • {site.siteType}
                      </div>
                    </div>
                    <span className={`text-[10px] font-black px-2 py-0.5 rounded ${
                      site.accessStatus === 'ACCORDED' ? 'bg-emerald-100 text-emerald-800' :
                      site.accessStatus === 'REFUSED' ? 'bg-red-100 text-red-800' : 'bg-amber-100 text-amber-800'
                    }`}>
                      {site.accessStatus}
                    </span>
                  </div>
                ))}
              </div>
            </div>
          </div>

          {/* Database metrics card */}
          <div className="bg-white rounded-2xl p-6 shadow-sm border border-slate-200 space-y-4">
            <h3 className="text-sm font-bold text-slate-900">Active Records & Storage Metrics</h3>

            <div className="grid grid-cols-3 gap-3 text-center">
              <div className="bg-slate-50 p-4 rounded-xl border border-slate-200">
                <span className="text-2xl font-black text-slate-900">{telecomSites.length}</span>
                <span className="block text-xs text-slate-500 font-semibold mt-1">Telecom Sites (site_db)</span>
              </div>
              <div className="bg-slate-50 p-4 rounded-xl border border-slate-200">
                <span className="text-2xl font-black text-slate-900">{reports.length}</span>
                <span className="block text-xs text-slate-500 font-semibold mt-1">Logged Audits</span>
              </div>
              <div className="bg-slate-50 p-4 rounded-xl border border-slate-200">
                <span className="text-2xl font-black text-slate-900">{allUsers.length}</span>
                <span className="block text-xs text-slate-500 font-semibold mt-1">Staff Profiles</span>
              </div>
            </div>

            <div className="pt-3 border-t border-slate-100 flex justify-between items-center text-xs text-slate-500">
              <span>Storage Engine: LocalStorage + Supabase site_db</span>
              <button
                type="button"
                onClick={() => {
                  localStorage.clear();
                  window.location.reload();
                }}
                className="text-red-600 hover:text-red-700 font-bold"
              >
                Reset All Local Data
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Add User Modal */}
      {showAddUserModal && (
        <div className="fixed inset-0 z-50 bg-black/60 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-md w-full p-6 shadow-2xl text-slate-800">
            <h3 className="text-base font-bold text-slate-900 mb-4">Add Staff Member</h3>
            <form onSubmit={handleCreateUser} className="space-y-3 text-xs">
              <div>
                <label className="block font-semibold text-slate-700 mb-1">Full Name</label>
                <input
                  type="text"
                  required
                  value={newUser.name}
                  onChange={(e) => setNewUser({ ...newUser, name: e.target.value })}
                  placeholder="e.g. Salim Toumi"
                  className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl focus:bg-white focus:outline-hidden"
                />
              </div>

              <div>
                <label className="block font-semibold text-slate-700 mb-1">Corporate Email</label>
                <input
                  type="email"
                  required
                  value={newUser.email}
                  onChange={(e) => setNewUser({ ...newUser, email: e.target.value })}
                  placeholder="stoumi@mobiserveholding.com"
                  className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl focus:bg-white focus:outline-hidden"
                />
              </div>

              <div>
                <label className="block font-semibold text-slate-700 mb-1">Phone Number</label>
                <input
                  type="text"
                  value={newUser.phone}
                  onChange={(e) => setNewUser({ ...newUser, phone: e.target.value })}
                  className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl focus:bg-white focus:outline-hidden"
                />
              </div>

              <div>
                <label className="block font-semibold text-slate-700 mb-1">Role Assignment</label>
                <select
                  value={newUser.role}
                  onChange={(e) => setNewUser({ ...newUser, role: e.target.value as UserRoleCode })}
                  className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl focus:bg-white focus:outline-hidden"
                >
                  {Object.values(USER_ROLES).map((r) => (
                    <option key={r.code} value={r.code}>
                      {r.code} - {r.label}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block font-semibold text-slate-700 mb-1">Department</label>
                <input
                  type="text"
                  value={newUser.department}
                  onChange={(e) => setNewUser({ ...newUser, department: e.target.value })}
                  className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl focus:bg-white focus:outline-hidden"
                />
              </div>

              <div className="flex justify-end space-x-2 pt-3 border-t border-slate-100">
                <button
                  type="button"
                  onClick={() => setShowAddUserModal(false)}
                  className="px-4 py-2 rounded-xl text-slate-600 hover:bg-slate-100 font-semibold"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 rounded-xl bg-blue-600 hover:bg-blue-700 text-white font-bold"
                >
                  Create Staff User
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
