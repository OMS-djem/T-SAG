import React from 'react';
import { useApp } from './context/AppContext';
import { Header } from './components/Header';
import { Navigation } from './components/Navigation';
import { CameraModal } from './components/CameraModal';
import { EmailPreviewModal } from './components/EmailPreviewModal';
import { SendResultModal } from './components/SendResultModal';
import { SignInScreen } from './screens/SignInScreen';
import { AccountPendingActivationScreen } from './screens/AccountPendingActivationScreen';
import { SiteAuditScreen } from './screens/SiteAuditScreen';
import { SitesMapScreen } from './screens/SitesMapScreen';
import { HistoryScreen } from './screens/HistoryScreen';
import { SettingsScreen } from './screens/SettingsScreen';

export const App: React.FC = () => {
  const { currentUser, isPendingActivation, activeTab } = useApp();

  // Auth Gate
  if (!currentUser) {
    return <SignInScreen />;
  }

  // Pending Activation Gate
  if (isPendingActivation) {
    return <AccountPendingActivationScreen />;
  }

  return (
    <div className="min-h-screen bg-slate-100 flex flex-col selection:bg-blue-600 selection:text-white">
      <Header />
      <Navigation />

      <main className="flex-1 w-full overflow-y-auto">
        {activeTab === 'AUDIT' && <SiteAuditScreen />}
        {activeTab === 'MAP' && <SitesMapScreen />}
        {activeTab === 'HISTORY' && <HistoryScreen />}
        {activeTab === 'SETTINGS' && <SettingsScreen />}
      </main>

      {/* Global Modals */}
      <CameraModal />
      <EmailPreviewModal />
      <SendResultModal />
    </div>
  );
};
