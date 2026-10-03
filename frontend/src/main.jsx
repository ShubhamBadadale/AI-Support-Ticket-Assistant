import React, { useEffect, useState } from 'react';
import { createRoot } from 'react-dom/client';
import { BrowserRouter, Routes, Route, Link, useNavigate, useParams } from 'react-router-dom';

const api = async (path, opts = {}) => {
  const r = await fetch(path, { credentials: 'include', headers: { 'Content-Type': 'application/json' }, ...opts });
  if (!r.ok) throw new Error(await r.text());
  return r.json();
};

function Login() {
  const [u, setU] = useState('customer1'); const [p, setP] = useState('password'); const nav = useNavigate();
  return (<div style={{ padding: 24 }}><h2>Sign in</h2>
    <p>Demo: customer1/password, agent1/password, admin/password</p>
    <input aria-label="username" value={u} onChange={e => setU(e.target.value)} />{' '}
    <input aria-label="password" type="password" value={p} onChange={e => setP(e.target.value)} />{' '}
    <button onClick={async () => { await api('/api/auth/login', { method: 'POST', body: JSON.stringify({ username: u, password: p }) }); nav('/'); }}>Sign in</button>
  </div>);
}

function Home() {
  const [me, setMe] = useState(null); const [tickets, setTickets] = useState([]);
  useEffect(() => { api('/api/auth/me').then(setMe).catch(() => window.location.hash = '#/login'); api('/api/tickets?size=20').then(d => setTickets(d.content || [])).catch(() => {}); }, []);
  if (!me) return <p>Loading…</p>;
  const isCustomer = me.role === 'CUSTOMER';
  return (<div style={{ padding: 24 }}>
    <h2>{isCustomer ? 'My tickets' : 'Support queue'} — {me.username} ({me.role})</h2>
    <nav><Link to="/new">Create ticket</Link> | <Link to="/knowledge">Knowledge</Link> | <Link to="/reports">Reports</Link></nav>
    <ul>{tickets.map(t => <li key={t.id}><Link to={`/tickets/${t.id}`}>{t.ticketNumber} — {t.subject} [{t.status}]</Link> ({t.confirmedCategory}/{t.confirmedUrgency}) {t.assignee ? 'assignee:' + t.assignee : ''}</li>)}</ul>
  </div>);
}

function NewTicket() {
  const [s, setS] = useState(''); const [d, setD] = useState(''); const nav = useNavigate();
  return (<div style={{ padding: 24 }}><h2>Create ticket</h2>
    <div><label>Subject (5-150): <input value={s} onChange={e => setS(e.target.value)} size={50} /></label></div>
    <div><label>Description (20-5000):<br /><textarea value={d} onChange={e => setD(e.target.value)} rows={6} cols={70} /></label></div>
    <button onClick={async () => { const t = await api('/api/tickets', { method: 'POST', body: JSON.stringify({ subject: s, description: d }) }); nav('/tickets/' + t.id); }}>Submit</button>
  </div>);
}

function Detail() {
  const { id } = useParams(); const [t, setT] = useState(null); const [msg, setMsg] = useState(''); const [draftEdit, setDraftEdit] = useState({});
  const load = () => api('/api/tickets/' + id).then(setT);
  useEffect(() => { load(); const h = setInterval(load, 3000); return () => clearInterval(h); }, [id]);
  if (!t) return <p>Loading… (AI jobs run in background)</p>;
  return (<div style={{ padding: 24 }}>
    <h2>{t.ticketNumber} — {t.subject}</h2>
    <p>Status: {t.status} | Category: {t.confirmedCategory} | Urgency: {t.confirmedUrgency} | conv v{t.conversationVersion}</p>
    <h3>Conversation</h3>
    <ul>{(t.messages || []).map(m => <li key={m.id}><b>{m.fromAgent ? 'AGENT' : 'CUSTOMER'} {m.author}:</b> {m.body}</li>)}</ul>
    {(t.internalNotes || []).length > 0 && (<><h3>Internal notes (staff only)</h3><ul>{t.internalNotes.map(n => <li key={n.id}>{n.author}: {n.body}</li>)}</ul></>)}
    {(t.triageSuggestions || []).length > 0 && (<><h3>AI triage suggestions</h3><ul>{t.triageSuggestions.map(s =>
      <li key={s.id}>{s.category} ({s.categoryReason}) / {s.urgency} ({s.urgencyReason}) v{s.conversationVersion} {s.stale ? '[STALE]' : ''} {s.applied ? '[APPLIED]' : ''}
        <button onClick={() => api(`/api/tickets/${id}/triage/${s.id}/apply`, { method: 'POST', body: JSON.stringify({}) }).then(load)}>Apply</button></li>)}</ul></>)}
    {(t.drafts || []).length > 0 && (<><h3>Drafts (AI-labeled, review required)</h3>{t.drafts.map(d =>
      <div key={d.id} style={{ border: '1px solid #ccc', padding: 8, margin: 8 }}>
        <p><b>{d.status}</b> {d.needsClarification ? '[needs clarification]' : ''} model={d.modelId}</p>
        <textarea value={draftEdit[d.id] ?? d.draftText} onChange={e => setDraftEdit({ ...draftEdit, [d.id]: e.target.value })} rows={5} cols={80} />
        <div>Citations: {(d.citations || []).map((c, i) => <span key={i}>[{c.title}: {String(c.excerpt).slice(0, 80)}…] </span>)}</div>
        <button onClick={() => api(`/api/drafts/${d.id}`, { method: 'PUT', body: JSON.stringify({ draftText: draftEdit[d.id] ?? d.draftText }) }).then(load)}>Save edit</button>{' '}
        <button onClick={() => api(`/api/drafts/${d.id}/send`, { method: 'POST', body: JSON.stringify({ finalText: draftEdit[d.id] ?? d.draftText, idempotencyKey: 'k-' + d.id }) }).then(load)}>Send reply (explicit)</button>{' '}
        <button onClick={() => api(`/api/drafts/${d.id}/reject`, { method: 'POST' }).then(load)}>Reject</button>
      </div>)}</>)}
    <div><button onClick={() => api(`/api/tickets/${id}/drafts`, { method: 'POST', body: JSON.stringify({}) }).then(load)}>Request draft</button>{' '}
      <button onClick={() => api(`/api/tickets/${id}/claim`, { method: 'POST' }).then(load)}>Claim</button></div>
    <h3>Reply</h3>
    <textarea aria-label="reply" value={msg} onChange={e => setMsg(e.target.value)} rows={3} cols={70} />
    <button onClick={() => api(`/api/tickets/${id}/messages`, { method: 'POST', body: JSON.stringify({ body: msg, internal: false }) }).then(() => { setMsg(''); load(); })}>Add message</button>
    <p><Link to="/">Back</Link></p>
  </div>);
}

function Knowledge() {
  const [arts, setArts] = useState([]); const [title, setTitle] = useState(''); const [content, setContent] = useState('');
  useEffect(() => { api('/api/articles').then(setArts).catch(() => {}); }, []);
  return (<div style={{ padding: 24 }}><h2>Knowledge management</h2>
    <ul>{arts.map(a => <li key={a.id}>{a.title} active={String(a.active)} published={a.publishedVersionId} index={a.latestStatus}</li>)}</ul>
    <h3>New article (admin)</h3>
    <input placeholder="title 5-150" value={title} onChange={e => setTitle(e.target.value)} size={50} /><br />
    <textarea placeholder="content 50-20000" value={content} onChange={e => setContent(e.target.value)} rows={5} cols={70} /><br />
    <button onClick={() => api('/api/articles', { method: 'POST', body: JSON.stringify({ title, content }) }).then(() => window.location.reload())}>Create + index</button>
    <p><Link to="/">Back</Link></p></div>);
}

function Reports() {
  const [r, setR] = useState(null);
  useEffect(() => { api('/api/admin/reports/summary').then(setR).catch(e => setR({ error: String(e) })); }, []);
  return (<div style={{ padding: 24 }}><h2>Admin reporting</h2><pre>{JSON.stringify(r, null, 2)}</pre><Link to="/">Back</Link></div>);
}

createRoot(document.getElementById('root')).render(
  <BrowserRouter><Routes>
    <Route path="/login" element={<Login />} />
    <Route path="/new" element={<NewTicket />} />
    <Route path="/tickets/:id" element={<Detail />} />
    <Route path="/knowledge" element={<Knowledge />} />
    <Route path="/reports" element={<Reports />} />
    <Route path="/" element={<Home />} />
  </Routes></BrowserRouter>);
