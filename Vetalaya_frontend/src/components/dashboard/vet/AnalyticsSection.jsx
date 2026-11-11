import React, { useState } from 'react';
import { 
  BarChart3, TrendingUp, Users, Calendar, DollarSign, 
  Activity, CheckCircle2, AlertCircle, MessageSquare, 
  Clock, ShieldAlert, Award, FileText, ArrowUpRight
} from 'lucide-react';

export default function AnalyticsSection() {
  const [remindersSent, setRemindersSent] = useState({});

  const kpis = [
    { label: "Today's Consultations", value: "18", change: "+12% vs last week", trend: "up", icon: Activity, color: "text-blue-600 bg-blue-50" },
    { label: "Prescriptions Dispensed", value: "15", change: "100% digital Rx", trend: "up", icon: FileText, color: "text-emerald-600 bg-emerald-50" },
    { label: "Vaccines Logged", value: "7", change: "3 Rabies certifications", trend: "up", icon: ShieldAlert, color: "text-purple-600 bg-purple-50" },
    { label: "Today's OPD Revenue", value: "₹ 11,400", change: "Avg ₹633/patient", trend: "up", icon: DollarSign, color: "text-amber-600 bg-amber-50" },
  ];

  const topPathologies = [
    { name: 'Canine Parvoviral Enteritis (CPV)', count: 14, percentage: 28, tag: 'Infectious' },
    { name: 'Tick-Borne Haemoparasites (Ehrlichia / Babesia)', count: 11, percentage: 22, tag: 'Vector-Borne' },
    { name: 'Allergic Dermatitis & Malassezia', count: 9, percentage: 18, tag: 'Dermatology' },
    { name: 'Bilateral Otitis Externa', count: 7, percentage: 14, tag: 'ENT' },
    { name: 'Gastroenteritis & Indiscretion', count: 5, percentage: 10, tag: 'Internal Med' },
    { name: 'Other Clinical Presentations', count: 4, percentage: 8, tag: 'General' },
  ];

  const upcomingReminders = [
    {
      id: 1,
      petName: 'Bruno',
      species: 'Dog (Golden Retriever)',
      owner: 'Rohit Kulkarni',
      phone: '+91 9921128469',
      treatment: 'Annual Anti-Rabies Booster (Rabisin)',
      dueDate: 'Tomorrow (Oct 14)',
      daysLeft: 1,
      urgency: 'high'
    },
    {
      id: 2,
      petName: 'Leo',
      species: 'Dog (German Shepherd)',
      owner: 'Aditya Patil',
      phone: '+91 9822119022',
      treatment: 'Nobivac DHPPi+L Booster #2',
      dueDate: 'In 3 days (Oct 16)',
      daysLeft: 3,
      urgency: 'medium'
    },
    {
      id: 3,
      petName: 'Bella',
      species: 'Cat (Persian)',
      owner: 'Pooja Nair',
      phone: '+91 9766554321',
      treatment: 'Deworming Follow-up (Drontal Cat)',
      dueDate: 'In 5 days (Oct 18)',
      daysLeft: 5,
      urgency: 'normal'
    },
    {
      id: 4,
      petName: 'Simba',
      species: 'Dog (Shih Tzu)',
      owner: 'Sameer Joshi',
      phone: '+91 9890123456',
      treatment: 'Post-Surgical Suture Removal',
      dueDate: 'In 2 days (Oct 15)',
      daysLeft: 2,
      urgency: 'high'
    }
  ];

  const sendWhatsAppReminder = (item) => {
    const text = encodeURIComponent(
      `Hello ${item.owner}, this is a gentle reminder from Dr. Clinic for ${item.petName}. ` +
      `The scheduled ${item.treatment} is due on ${item.dueDate}. ` +
      `Please visit the clinic or reply to schedule your slot. Powered by Vetalya.`
    );
    const url = `https://wa.me/${item.phone.replace(/[^0-9]/g, '')}?text=${text}`;
    window.open(url, '_blank');
    setRemindersSent(prev => ({ ...prev, [item.id]: true }));
  };

  return (
    <div className="space-y-6">
      {/* Top Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 bg-white p-6 rounded-2xl border border-gray-200 shadow-sm">
        <div>
          <h2 className="text-xl font-bold text-gray-900 flex items-center gap-2">
            <BarChart3 className="w-6 h-6 text-emerald-600" />
            <span>Practice Performance & Clinical Disease Registry</span>
          </h2>
          <p className="text-sm text-gray-500 mt-1">
            Real-time practice analytics, disease surveillance and proactive 7-day preventive reminder queue.
          </p>
        </div>
        <div className="flex items-center gap-2 text-xs font-semibold px-3 py-1.5 bg-emerald-50 text-emerald-700 rounded-lg border border-emerald-200 self-start sm:self-center">
          <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse"></span>
          <span>Live Clinical Stream Active</span>
        </div>
      </div>

      {/* KPI Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {kpis.map((kpi, idx) => {
          const Icon = kpi.icon;
          return (
            <div key={idx} className="bg-white border border-gray-200 rounded-2xl p-5 shadow-sm hover:shadow-md transition-shadow">
              <div className="flex items-center justify-between">
                <span className="text-xs font-semibold text-gray-500">{kpi.label}</span>
                <div className={`p-2 rounded-xl ${kpi.color}`}>
                  <Icon className="w-4 h-4" />
                </div>
              </div>
              <div className="mt-3 flex items-baseline justify-between">
                <span className="text-2xl font-black text-gray-900">{kpi.value}</span>
                <span className="text-xs font-medium text-emerald-600 flex items-center">
                  <ArrowUpRight className="w-3.5 h-3.5" />
                  {kpi.change}
                </span>
              </div>
            </div>
          );
        })}
      </div>

      {/* Main Grid: Disease Epidemiology + 7-Day Reminder Dispatch */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        {/* LEFT: Disease Surveillance Breakdown (6 cols) */}
        <div className="lg:col-span-6 bg-white border border-gray-200 rounded-2xl p-6 shadow-sm">
          <div className="flex items-center justify-between mb-5">
            <div>
              <h3 className="font-bold text-gray-900 flex items-center gap-2">
                <TrendingUp className="w-5 h-5 text-blue-600" />
                <span>Top Clinical Pathologies (This Month)</span>
              </h3>
              <p className="text-xs text-gray-500 mt-0.5">Distribution based on finalized SOAP diagnoses</p>
            </div>
            <span className="text-xs font-mono font-bold px-2 py-1 bg-gray-100 rounded-md text-gray-700">
              N = 50 Cases
            </span>
          </div>

          <div className="space-y-4">
            {topPathologies.map((item, idx) => (
              <div key={idx} className="space-y-1.5">
                <div className="flex items-center justify-between text-xs">
                  <span className="font-bold text-gray-800 flex items-center gap-1.5">
                    <span className="text-gray-400 font-mono">#{idx + 1}</span>
                    {item.name}
                  </span>
                  <div className="flex items-center gap-2">
                    <span className="px-1.5 py-0.5 bg-gray-100 text-gray-600 rounded text-[10px] font-semibold">
                      {item.tag}
                    </span>
                    <span className="font-bold text-gray-900">{item.count} ({item.percentage}%)</span>
                  </div>
                </div>
                {/* Progress bar */}
                <div className="w-full bg-gray-100 rounded-full h-2.5 overflow-hidden">
                  <div 
                    className={`h-2.5 rounded-full ${
                      idx === 0 ? 'bg-red-500' :
                      idx === 1 ? 'bg-amber-500' :
                      idx === 2 ? 'bg-blue-500' :
                      idx === 3 ? 'bg-purple-500' : 'bg-emerald-500'
                    }`}
                    style={{ width: `${item.percentage}%` }}
                  ></div>
                </div>
              </div>
            ))}
          </div>

          <div className="mt-6 p-3.5 bg-blue-50 border border-blue-200 rounded-xl text-xs text-blue-900 flex items-center gap-2">
            <span className="font-bold">Epidemiological Alert:</span>
            <span>Canine Parvo cases increased by 15% this season. Recommend proactive primary vaccination for puppies &lt;16 weeks.</span>
          </div>
        </div>

        {/* RIGHT: 7-Day Preventive Reminder Dispatch (6 cols) */}
        <div className="lg:col-span-6 bg-white border border-gray-200 rounded-2xl p-6 shadow-sm">
          <div className="flex items-center justify-between mb-5">
            <div>
              <h3 className="font-bold text-gray-900 flex items-center gap-2">
                <Calendar className="w-5 h-5 text-purple-600" />
                <span>7-Day Preventive & Booster Queue</span>
              </h3>
              <p className="text-xs text-gray-500 mt-0.5">Automated vaccination and post-op follow-up alerts</p>
            </div>
            <span className="px-2.5 py-1 bg-purple-100 text-purple-800 text-xs font-bold rounded-full">
              {upcomingReminders.length} Scheduled
            </span>
          </div>

          <div className="space-y-3">
            {upcomingReminders.map((item) => {
              const isSent = remindersSent[item.id];
              return (
                <div key={item.id} className="p-3.5 rounded-xl border border-gray-200 hover:border-gray-300 bg-gray-50/50 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                  <div>
                    <div className="flex items-center gap-2">
                      <span className="font-bold text-sm text-gray-900">{item.petName}</span>
                      <span className="text-xs text-gray-500">({item.species})</span>
                      <span className={`text-[10px] font-bold px-2 py-0.5 rounded-full ${
                        item.urgency === 'high' ? 'bg-red-100 text-red-700' :
                        item.urgency === 'medium' ? 'bg-amber-100 text-amber-700' : 'bg-blue-100 text-blue-700'
                      }`}>
                        {item.dueDate}
                      </span>
                    </div>
                    <p className="text-xs font-medium text-gray-700 mt-1">{item.treatment}</p>
                    <p className="text-[11px] text-gray-500 mt-0.5">Parent: {item.owner} ({item.phone})</p>
                  </div>

                  <button
                    onClick={() => sendWhatsAppReminder(item)}
                    className={`px-3 py-1.5 rounded-xl text-xs font-bold flex items-center gap-1.5 transition-all self-start sm:self-center cursor-pointer ${
                      isSent 
                        ? 'bg-emerald-100 text-emerald-800 border border-emerald-300' 
                        : 'bg-green-600 hover:bg-green-700 text-white shadow-sm'
                    }`}
                  >
                    {isSent ? (
                      <>
                        <CheckCircle2 className="w-3.5 h-3.5" />
                        <span>Sent</span>
                      </>
                    ) : (
                      <>
                        <MessageSquare className="w-3.5 h-3.5" />
                        <span>WhatsApp Alert</span>
                      </>
                    )}
                  </button>
                </div>
              );
            })}
          </div>

          {/* Quick Info */}
          <div className="mt-5 p-3.5 bg-gray-50 border border-gray-200 rounded-xl flex items-center justify-between text-xs text-gray-600">
            <span>Avg follow-up adherence rate: <strong className="text-gray-900 font-semibold">91.4%</strong></span>
            <span className="font-semibold text-emerald-600">Client retention +24%</span>
          </div>
        </div>
      </div>
    </div>
  );
}

