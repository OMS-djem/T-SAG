import React from 'react';
import { useApp } from '../context/AppContext';
import { CheckCircle, Clock, AlertOctagon, X } from 'lucide-react';

export const SendResultModal: React.FC = () => {
  const { sendResultDialog, closeSendResultDialog, setActiveTab } = useApp();

  if (!sendResultDialog.show) return null;

  const isSuccess = sendResultDialog.success;

  return (
    <div className="fixed inset-0 z-50 bg-black/60 backdrop-blur-xs flex items-center justify-center p-4">
      <div className="bg-white rounded-2xl max-w-md w-full p-6 shadow-2xl text-slate-800 animate-in fade-in zoom-in-95">
        <div className="flex items-start justify-between mb-4">
          <div className="flex items-center space-x-3">
            {isSuccess ? (
              <div className="w-11 h-11 rounded-full bg-emerald-100 text-emerald-600 flex items-center justify-center shrink-0">
                <CheckCircle className="w-6 h-6" />
              </div>
            ) : sendResultDialog.title.includes('Offline') || sendResultDialog.title.includes('Queued') ? (
              <div className="w-11 h-11 rounded-full bg-amber-100 text-amber-600 flex items-center justify-center shrink-0">
                <Clock className="w-6 h-6" />
              </div>
            ) : (
              <div className="w-11 h-11 rounded-full bg-red-100 text-red-600 flex items-center justify-center shrink-0">
                <AlertOctagon className="w-6 h-6" />
              </div>
            )}
            <div>
              <h3 className="text-base font-bold text-slate-900">{sendResultDialog.title}</h3>
              <p className="text-xs text-slate-500">Mobiserve Telecom NOC Audit Dispatch</p>
            </div>
          </div>
          <button
            onClick={closeSendResultDialog}
            className="p-1 rounded-full hover:bg-slate-100 text-slate-400 hover:text-slate-600"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <p className="text-sm text-slate-700 font-medium mb-3">
          {sendResultDialog.message}
        </p>

        {sendResultDialog.details && (
          <div className="bg-slate-900 text-emerald-400 font-mono text-[11px] p-3 rounded-xl border border-slate-800 mb-5 leading-relaxed overflow-x-auto whitespace-pre-wrap">
            {sendResultDialog.details}
          </div>
        )}

        <div className="flex justify-end space-x-2">
          <button
            type="button"
            onClick={() => {
              closeSendResultDialog();
              setActiveTab('HISTORY');
            }}
            className="px-4 py-2 rounded-xl text-xs font-semibold bg-slate-100 hover:bg-slate-200 text-slate-700 transition-colors"
          >
            View in History Logs
          </button>
          <button
            type="button"
            onClick={closeSendResultDialog}
            className="px-4 py-2 rounded-xl text-xs font-bold bg-blue-600 hover:bg-blue-700 text-white shadow transition-colors"
          >
            Dismiss
          </button>
        </div>
      </div>
    </div>
  );
};
