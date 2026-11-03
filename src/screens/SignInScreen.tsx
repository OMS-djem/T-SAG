import React, { useState } from 'react';
import { useApp } from '../context/AppContext';
import { UserRoleCode, USER_ROLES } from '../types';
import { 
  RadioTower, 
  Lock, 
  Mail, 
  User, 
  Phone, 
  Briefcase, 
  MapPin, 
  ShieldCheck, 
  ArrowRight,
  Eye,
  EyeOff
} from 'lucide-react';

const DEMO_USERS: Array<{ name: string; email: string; role: UserRoleCode; description: string }> = [
  {
    name: 'M. Djemil',
    email: 'mdjemil@mobiserveholding.com',
    role: 'ADMIN',
    description: 'System Administrator (Full access to settings & user activation)'
  },
  {
    name: 'Karim Benali',
    email: 'kbenali@mobiserveholding.com',
    role: 'TECHNICIAN',
    description: 'Field Technician (Site audits, photo watermarking, GPS logs)'
  },
  {
    name: 'Samir Hadj',
    email: 'shadj@mobiserveholding.com',
    role: 'OM',
    description: 'Operations & Maintenance Field Engineer'
  },
  {
    name: 'Amine Mansour',
    email: 'amansour@mobiserveholding.com',
    role: 'SUPERVISION',
    description: 'Regional Supervision & NOC Coordinator'
  }
];

export const SignInScreen: React.FC = () => {
  const { signIn, signUp } = useApp();

  const [authTab, setAuthTab] = useState<'LOGIN' | 'REGISTER'>('LOGIN');

  // Login inputs
  const [loginEmail, setLoginEmail] = useState('mdjemil@mobiserveholding.com');
  const [loginPassword, setLoginPassword] = useState('123456');
  const [showLoginPassword, setShowLoginPassword] = useState(false);

  // Register inputs
  const [regName, setRegName] = useState('');
  const [regEmail, setRegEmail] = useState('');
  const [regPassword, setRegPassword] = useState('');
  const [regPhone, setRegPhone] = useState('+213 550 ');
  const [regDepartment, setRegDepartment] = useState('Telecom Field Operations');
  const [regRegion, setRegRegion] = useState('Central - Algiers');
  const [regRole, setRegRole] = useState<UserRoleCode>('TECHNICIAN');

  const [isLoading, setIsLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const handleLoginSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);
    setErrorMessage(null);
    const result = await signIn(loginEmail, loginPassword);
    setIsLoading(false);
    if (!result.success) {
      setErrorMessage(result.message || 'Login failed.');
    }
  };

  const handleRegisterSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);
    setErrorMessage(null);
    const result = await signUp({
      name: regName,
      email: regEmail,
      phone: regPhone,
      department: regDepartment,
      region: regRegion,
      role: regRole
    });
    setIsLoading(false);
    if (!result.success) {
      setErrorMessage(result.message);
    }
  };

  const handleSelectDemo = async (email: string) => {
    setLoginEmail(email);
    setIsLoading(true);
    await signIn(email);
    setIsLoading(false);
  };

  return (
    <div className="min-h-screen bg-gradient-to-b from-telecom-navy via-slate-900 to-slate-950 text-white flex flex-col justify-center items-center px-4 py-8">
      <div className="max-w-md w-full space-y-6">
        {/* Brand Header */}
        <div className="text-center space-y-2">
          <div className="w-16 h-16 rounded-2xl bg-blue-600/30 border border-blue-400/40 flex items-center justify-center text-sky-400 mx-auto shadow-2xl">
            <RadioTower className="w-9 h-9" />
          </div>
          <h1 className="text-2xl font-black tracking-tight text-white">MOBISERVE HOLDING</h1>
          <p className="text-xs text-sky-400 font-bold uppercase tracking-wider">
            T-SAG • Telecom Site Access Gateway
          </p>
          <p className="text-xs text-slate-400 max-w-xs mx-auto">
            Audit logging with certified GPS telemetry, GSM cellular metrics, and Zoho Mail dispatch
          </p>
        </div>

        {/* Auth Card */}
        <div className="bg-white rounded-2xl shadow-2xl p-6 text-slate-800 space-y-5">
          {/* Tabs */}
          <div className="grid grid-cols-2 gap-2 bg-slate-100 p-1 rounded-xl">
            <button
              type="button"
              onClick={() => {
                setAuthTab('LOGIN');
                setErrorMessage(null);
              }}
              className={`py-2 text-xs font-bold rounded-lg transition-all ${
                authTab === 'LOGIN'
                  ? 'bg-white text-slate-900 shadow-xs'
                  : 'text-slate-500 hover:text-slate-800'
              }`}
            >
              Sign In Session
            </button>
            <button
              type="button"
              onClick={() => {
                setAuthTab('REGISTER');
                setErrorMessage(null);
              }}
              className={`py-2 text-xs font-bold rounded-lg transition-all ${
                authTab === 'REGISTER'
                  ? 'bg-white text-slate-900 shadow-xs'
                  : 'text-slate-500 hover:text-slate-800'
              }`}
            >
              Create Account
            </button>
          </div>

          {errorMessage && (
            <div className="bg-red-50 border border-red-200 text-red-700 text-xs p-3 rounded-xl font-medium">
              {errorMessage}
            </div>
          )}

          {/* Login Form */}
          {authTab === 'LOGIN' ? (
            <form onSubmit={handleLoginSubmit} className="space-y-3.5 text-xs">
              <div>
                <label className="block font-semibold text-slate-700 mb-1">Corporate Email</label>
                <div className="relative">
                  <Mail className="w-4 h-4 text-slate-400 absolute left-3 top-2.5" />
                  <input
                    type="email"
                    required
                    value={loginEmail}
                    onChange={(e) => setLoginEmail(e.target.value)}
                    placeholder="user@mobiserveholding.com"
                    className="w-full pl-9 pr-3 py-2 bg-slate-50 border border-slate-200 rounded-xl focus:bg-white focus:outline-hidden"
                  />
                </div>
              </div>

              <div>
                <label className="block font-semibold text-slate-700 mb-1">Password</label>
                <div className="relative">
                  <Lock className="w-4 h-4 text-slate-400 absolute left-3 top-2.5" />
                  <input
                    type={showLoginPassword ? 'text' : 'password'}
                    required
                    value={loginPassword}
                    onChange={(e) => setLoginPassword(e.target.value)}
                    placeholder="••••••••"
                    className="w-full pl-9 pr-9 py-2 bg-slate-50 border border-slate-200 rounded-xl focus:bg-white focus:outline-hidden"
                  />
                  <button
                    type="button"
                    onClick={() => setShowLoginPassword(!showLoginPassword)}
                    className="absolute right-3 top-2.5 text-slate-400 hover:text-slate-600"
                  >
                    {showLoginPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                  </button>
                </div>
              </div>

              <button
                type="submit"
                disabled={isLoading}
                className="w-full py-2.5 rounded-xl bg-blue-600 hover:bg-blue-700 text-white font-bold text-xs shadow-md transition-colors flex items-center justify-center disabled:opacity-50"
              >
                Sign In to T-SAG <ArrowRight className="w-4 h-4 ml-1.5" />
              </button>
            </form>
          ) : (
            /* Register Form */
            <form onSubmit={handleRegisterSubmit} className="space-y-3 text-xs">
              <div>
                <label className="block font-semibold text-slate-700 mb-1">Full Name</label>
                <input
                  type="text"
                  required
                  value={regName}
                  onChange={(e) => setRegName(e.target.value)}
                  placeholder="e.g. Youcef Larbi"
                  className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl focus:bg-white focus:outline-hidden"
                />
              </div>

              <div>
                <label className="block font-semibold text-slate-700 mb-1">Corporate Email</label>
                <input
                  type="email"
                  required
                  value={regEmail}
                  onChange={(e) => setRegEmail(e.target.value)}
                  placeholder="ylarbi@mobiserveholding.com"
                  className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl focus:bg-white focus:outline-hidden"
                />
              </div>

              <div>
                <label className="block font-semibold text-slate-700 mb-1">Password</label>
                <input
                  type="password"
                  required
                  value={regPassword}
                  onChange={(e) => setRegPassword(e.target.value)}
                  placeholder="Create session password"
                  className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl focus:bg-white focus:outline-hidden"
                />
              </div>

              <div className="grid grid-cols-2 gap-2">
                <div>
                  <label className="block font-semibold text-slate-700 mb-1">Phone</label>
                  <input
                    type="text"
                    value={regPhone}
                    onChange={(e) => setRegPhone(e.target.value)}
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl focus:bg-white focus:outline-hidden"
                  />
                </div>

                <div>
                  <label className="block font-semibold text-slate-700 mb-1">Role</label>
                  <select
                    value={regRole}
                    onChange={(e) => setRegRole(e.target.value as UserRoleCode)}
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl focus:bg-white focus:outline-hidden"
                  >
                    {Object.values(USER_ROLES).map((r) => (
                      <option key={r.code} value={r.code}>{r.label}</option>
                    ))}
                  </select>
                </div>
              </div>

              <button
                type="submit"
                disabled={isLoading}
                className="w-full py-2.5 rounded-xl bg-blue-600 hover:bg-blue-700 text-white font-bold text-xs shadow-md transition-colors flex items-center justify-center disabled:opacity-50"
              >
                Register Account
              </button>
            </form>
          )}

          {/* Quick Demo Switcher */}
          <div className="pt-3 border-t border-slate-100">
            <span className="block text-[11px] font-bold uppercase tracking-wider text-slate-400 mb-2">
              Quick Test Profile Switcher:
            </span>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
              {DEMO_USERS.map((demo) => {
                const roleInfo = USER_ROLES[demo.role];
                return (
                  <button
                    key={demo.email}
                    type="button"
                    onClick={() => handleSelectDemo(demo.email)}
                    className="p-2.5 text-left rounded-xl border border-slate-200 hover:border-blue-400 hover:bg-blue-50/50 transition-all flex flex-col group"
                  >
                    <div className="flex justify-between items-center mb-0.5">
                      <span className="font-bold text-slate-900 group-hover:text-blue-700 text-xs">{demo.name}</span>
                      <span 
                        className="text-[9px] font-black text-white px-1.5 py-0.2 rounded"
                        style={{ backgroundColor: roleInfo.color }}
                      >
                        {demo.role}
                      </span>
                    </div>
                    <span className="text-[10px] text-slate-500 line-clamp-1">{demo.description}</span>
                  </button>
                );
              })}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
