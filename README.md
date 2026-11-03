# Telecom Site Access (T-SAG) - React Web Edition

A field engineering application for telecom site access audits, physical security inspections, and real-time incident escalation. Built with **React 18**, **TypeScript**, **Tailwind CSS**, and **Leaflet**.

---

## Key Features

1. **Site Access Audit & Verdict Logging**
   - Instant verdict logging: **ACCESS ACCORDED ✓** (Entry granted) or **NO ACCESS / ACCESS REFUSED ✕** (Entry blocked).
   - Structured refusal reason documentation (missing permits, padlocks changed, building syndicate refusal, hostile disputes).
   - Work notes and observations field.

2. **Real-time Field Telemetry**
   - **GPS Coordinates Stamp**: High-precision latitude, longitude, altitude (MSL), and accuracy radius with Google Maps deep link.
   - **GSM & Cellular Telemetry**: Connected network type (LTE/4G+, 5G NR), operator identification (Mobilis, Djezzy, Ooredoo), Cell ID (CID/ECI), LAC/TAC, MCC-MNC codes, and signal strength (dBm).

3. **Security Watermarked Photo Evidence**
   - Integrated camera with live HUD overlay matching field standards.
   - Canvas-stamped tamper-evident watermark displaying company header, site ID, operator, status color indicator, coordinates, accuracy, altitude, GSM cell telemetry, and timestamp.
   - Tag categorizer: Site Entrance, Telecom Tower, Equipment Shelter, Access Gate & Lock, Security Guard Post, Power Generator.

4. **Sites Map & Radar Proximity**
   - Interactive Leaflet OpenStreetMap with custom color-coded tower pins (Green = Accorded, Red = Refused, Amber = Pending).
   - Real-time technician GPS tracking with pulsing radar accuracy ring.
   - Nearest tower calculation with direct "Audit This Site" handoff.
   - Directory search and operator filtering chips.

5. **Offline Queue & Cloud Sync**
   - Automatic local storage queueing when offline.
   - One-tap queue synchronization when internet connectivity is restored.
   - Full history log with photo zoom and email copy.

6. **Zoho Mail SMTP Dispatch & Reporting**
   - Formatted corporate report generation with tabular ASCII layout.
   - Configurable Zoho SMTP routing (host, port, app password, CC supervision group).
   - Live SMTP connection test diagnostics.

7. **Role-Based Access Control (RBAC) & Activation Workflow**
   - 6 Operational Roles: `ADMIN`, `OM`, `SUPERVISION`, `TECHNICIAN`, `DISPATCHER`, `ACQUISITION`.
   - Admin account approval holding gate for self-registered users.
   - Quick demo account switcher for rapid role testing.

---

## Development

```bash
# Install dependencies
npm install

# Start development server on port 3000
npm run dev

# Build production bundle
npm run build
```
