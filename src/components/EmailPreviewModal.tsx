import React, { useState } from 'react';
import { useApp } from '../context/AppContext';
import { EmailService } from '../services/email';
import { Mail, Copy, Check, X, Send, AlertTriangle } from 'lucide-react';

export const EmailPreviewModal: React.FC = () => {
  const {
    showEmailPreview,
    setShowEmailPreview,
    siteId,
    siteName,
    operator,
    status,
    refusalReason,
    notes,
    technicianName,
    technicianPhone,
    recipientEmail,
    ccRecipientsEmail,
    gpsData,
    gsmData,
    photos,
    sendAuditReport,
    isSendingEmail
  } = useApp();

  const [copied, setCopied] = useState(false);

  if (!showEmailPreview) return null;

  const subject = EmailService.generateSubject(siteId, operator, status);
  const body = EmailService.generateBody(
    siteId,
    siteName,
    operator,
    status,
    status === 'REFUSED' ? refusalReason : undefined,
    notes,
    technicianName,
    technicianPhone,
    gpsData,
    gsmData,
    photos.length
  );

  const copyToClipboard = () => {
    navigator.clipboard.writeText(`SUBJECT: ${subject}\n\n${body}`);
    setCopied(true);
    setTimeout(() => setCopied(false), 2500);
  };

  return (
    <div className="fixed inset-0 z-50 bg-black/60 backdrop-blur-xs flex items-center justify-center p-4">
      <div className="bg-white rounded-2xl max-w-2xl w-full max-h-[90vh] flex flex-col shadow-2xl overflow-hidden animate-in fade-in zoom-in-95">
        {/* Modal Header */}
        <div className="bg-telecom-navy text-white px-5 py-4 flex items-center justify-between border-b border-slate-700">
          <div className="flex items-center space-x-2">
            <Mail className="w-5 h-5 text-sky-400" />
            <div>
              <h3 className="text-base font-bold text-white">Zoho Mail Dispatch Preview</h3>
              <p className="text-xs text-slate-300">NOC Operations & Management Routing</p>
            </div>
          </div>
          <button
            onClick={() => setShowEmailPreview(false)}
            className="p-1.5 rounded-full hover:bg-white/10 text-white transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Modal Body */}
        <div className="p-5 flex-1 overflow-y-auto space-y-4 text-sm text-slate-700">
          {/* Email Header Metadata */}
          <div className="bg-slate-50 border border-slate-200 rounded-xl p-3.5 space-y-1.5 text-xs">
            <div className="flex items-center">
              <span className="w-16 font-semibold text-slate-500">To:</span>
              <span className="font-mono text-slate-800 bg-white px-2 py-0.5 rounded border border-slate-200 truncate">
                {recipientEmail}
              </span>
            </div>
            {ccRecipientsEmail && (
              <div className="flex items-center">
                <span className="w-16 font-semibold text-slate-500">CC:</span>
                <span className="font-mono text-slate-600 bg-white px-2 py-0.5 rounded border border-slate-200 truncate">
                  {ccRecipientsEmail}
                </span>
              </div>
            )}
            <div className="flex items-baseline pt-1 border-t border-slate-200">
              <span className="w-16 font-semibold text-slate-500">Subject:</span>
              <span className={`font-semibold ${status === 'ACCORDED' ? 'text-emerald-700' : 'text-red-700'}`}>
                {subject}
              </span>
            </div>
          </div>

          {/* Formatted Email Content */}
          <div className="relative">
            <pre className="font-mono text-xs bg-slate-900 text-slate-200 p-4 rounded-xl whitespace-pre-wrap leading-relaxed overflow-x-auto max-h-80 border border-slate-800 selection:bg-blue-500 selection:text-white">
              {body}
            </pre>
          </div>

          {/* Photos notice */}
          <div className="flex items-center space-x-2 text-xs text-slate-600 bg-blue-50 border border-blue-200 rounded-lg p-2.5">
            <AlertTriangle className="w-4 h-4 text-blue-600 shrink-0" />
            <span>
              <strong>{photos.length}</strong> photo evidence attachment(s) will be embedded with certified watermarks.
            </span>
          </div>
        </div>

        {/* Modal Actions */}
        <div className="bg-slate-50 border-t border-slate-200 px-5 py-3 flex items-center justify-between">
          <button
            type="button"
            onClick={copyToClipboard}
            className="px-3.5 py-2 rounded-xl border border-slate-300 hover:bg-slate-100 text-slate-700 text-xs font-semibold flex items-center transition-colors"
          >
            {copied ? (
              <>
                <Check className="w-4 h-4 text-emerald-600 mr-1.5" /> Copied!
              </>
            ) : (
              <>
                <Copy className="w-4 h-4 text-slate-500 mr-1.5" /> Copy Text
              </>
            )}
          </button>

          <div className="flex space-x-2">
            <button
              type="button"
              onClick={() => setShowEmailPreview(false)}
              className="px-4 py-2 rounded-xl text-xs font-semibold text-slate-600 hover:bg-slate-200 transition-colors"
            >
              Close
            </button>
            <button
              type="button"
              disabled={isSendingEmail}
              onClick={async () => {
                setShowEmailPreview(false);
                await sendAuditReport();
              }}
              className="px-4 py-2 rounded-xl text-xs font-bold bg-blue-600 hover:bg-blue-700 text-white flex items-center shadow-md transition-colors disabled:opacity-50"
            >
              <Send className="w-3.5 h-3.5 mr-1.5" /> Send to Zoho Now
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
