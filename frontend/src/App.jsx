import { useEffect, useRef, useState } from 'react';
import { api } from './api.js';
import FoodArt from './FoodArt.jsx';

const money = value => new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 }).format(value);
const suggestions = [
  { icon: 'leaf', title: 'Find my next meal', subtitle: 'Something veg under ₹200', prompt: 'Recommend a vegetarian meal from your menu under ₹200.' },
  { icon: 'sparkle', title: 'Cook with what I have', subtitle: 'A quick pantry recipe', prompt: 'I have rice, tomatoes and onions. Suggest an easy vegetarian dinner recipe with steps.' },
  { icon: 'clock', title: 'Where’s my order?', subtitle: 'Check my latest delivery', prompt: 'What is the status of my latest order?' },
  { icon: 'history', title: 'A little order help', subtitle: 'Delivery, fees & refunds', prompt: 'How do delivery fees and refunds work in this demo?' },
];

function Icon({ name, size = 20, ...props }) {
  const paths = {
    sparkle: <><path d="m12 3 2.5 6.5L21 12l-6.5 2.5L12 21l-2.5-6.5L3 12l6.5-2.5Z"/><path d="m20 2 .7 1.8L22.5 4l-1.8.7L20 6.5l-.7-1.8L17.5 4l1.8-.2Z"/></>,
    bag: <><path d="M5 7h14l1 14H4L5 7Z"/><path d="M8 7V6a4 4 0 0 1 8 0v1"/></>,
    search: <><circle cx="10.5" cy="10.5" r="6.5"/><path d="m16 16 4.5 4.5"/></>,
    arrow: <><path d="M5 12h14m-5-5 5 5-5 5"/></>,
    send: <><path d="m4 4 17 8-17 8 3-8-3-8Zm3 8h14"/></>,
    plus: <path d="M12 5v14M5 12h14"/>,
    minus: <path d="M5 12h14"/>,
    close: <path d="m6 6 12 12M6 18 18 6"/>,
    clock: <><circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/></>,
    check: <path d="m5 12 4 4L19 6"/>,
    leaf: <><path d="M5 19c-4-8 2-14 15-15 0 12-6 18-13 14"/><path d="m5 20 10-10"/></>,
    history: <><path d="M3 10a9 9 0 1 1 1.8 8M3 4v6h6"/><path d="M12 7v5l3 2"/></>,
    refresh: <><path d="M20 7A8.5 8.5 0 0 0 5 5l-2 3m0-5v5h5M4 17a8.5 8.5 0 0 0 15 2l2-3m0 5v-5h-5"/></>,
  };
  return <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true" {...props}>{paths[name] || paths.sparkle}</svg>;
}

function readSaved(key, fallback) {
  try { const value = JSON.parse(localStorage.getItem(key)); return value ?? fallback; } catch { return fallback; }
}

function FoodCard({ item, onAdd, compact = false }) {
  return <article className={`food-card ${compact ? 'compact' : ''}`}>
    <div className={`food-art ${item.color}`}><FoodArt dish={item.id} className="food-emoji"/><span className="food-tag">{item.tag}</span></div>
    <div className="food-info">
      <div className="food-meta"><span className={`diet-mark ${item.diet === 'Non-Vegetarian' ? 'non-veg' : ''}`} aria-label={item.diet} /> <span>{item.category}</span><span className="rating">★ {item.rating}</span></div>
      <h3>{item.name}</h3>{!compact && <p>{item.description}</p>}
      <div className="food-bottom"><strong>{money(item.price)}</strong><span className="minutes"><Icon name="clock" size={13} />{item.minutes} min</span><button className="add-button" aria-label={`Add ${item.name} to bag`} onClick={() => onAdd(item)}><Icon name="plus" size={17} /> Add</button></div>
    </div>
  </article>;
}

function OrderCard({ order, onAsk }) {
  const stages = ['Preparing', 'On the way', 'Delivered'];
  const current = stages.indexOf(order.status);
  return <article className="order-card">
    <div className="order-card-top"><div><span className="eyebrow">DEMO ORDER</span><h3>{order.id}</h3></div><span className="order-badge">{order.status}</span></div>
    <div className="order-items">{order.items.map(item => <div key={item.itemId}><span><FoodArt dish={item.itemId}/> {item.quantity} × {item.name}</span><strong>{money(item.price * item.quantity)}</strong></div>)}</div>
    <div className="order-steps">{stages.map((stage, index) => <div className={index <= current ? 'done' : ''} key={stage}><span>{index <= current ? <Icon name="check" size={12} /> : index + 1}</span><small>{stage}</small></div>)}</div>
    <div className="order-card-bottom"><strong>Total {money(order.total)}</strong><button className="text-button" onClick={() => onAsk(`Please check order ${order.id}.`)}>Ask Zuffy <Icon name="arrow" size={15} /></button></div>
    <p className="demo-note">Simulated delivery in 2 minutes. No payment or real delivery.</p>
  </article>;
}

export default function App() {
  const [sessionId] = useState(() => {
    let value = readSaved('zuffy-session', null);
    if (typeof value !== 'string' || !/^[a-zA-Z0-9_-]{8,64}$/.test(value)) value = crypto.randomUUID();
    localStorage.setItem('zuffy-session', JSON.stringify(value)); return value;
  });
  const [tab, setTab] = useState('assistant');
  const [menu, setMenu] = useState([]);
  const [health, setHealth] = useState(null);
  const [loadError, setLoadError] = useState('');
  const [menuLoading, setMenuLoading] = useState(true);
  const [messages, setMessages] = useState(() => {
    const saved = readSaved('zuffy-chat', []);
    return Array.isArray(saved) ? saved.filter(m => ['user', 'assistant'].includes(m.role) && typeof m.content === 'string').slice(-24) : [];
  });
  const [input, setInput] = useState('');
  const [busy, setBusy] = useState(false);
  const [chatError, setChatError] = useState('');
  const [cart, setCart] = useState(() => {
    const saved = readSaved('zuffy-cart', {});
    return saved && typeof saved === 'object' && !Array.isArray(saved) ? Object.fromEntries(Object.entries(saved).filter(([, q]) => Number.isInteger(q) && q > 0 && q <= 10)) : {};
  });
  const [bagOpen, setBagOpen] = useState(false);
  const [orders, setOrders] = useState([]);
  const [checkoutBusy, setCheckoutBusy] = useState(false);
  const [checkoutError, setCheckoutError] = useState('');
  const [orderError, setOrderError] = useState('');
  const [diet, setDiet] = useState(() => readSaved('zuffy-diet', 'All'));
  const [avoid, setAvoid] = useState(() => readSaved('zuffy-avoid', ''));
  const [category, setCategory] = useState('All');
  const [search, setSearch] = useState('');
  const [toast, setToast] = useState('');
  const toastTimer = useRef();
  const endRef = useRef();
  const textareaRef = useRef();
  const sendLock = useRef(false);
  const bagRef = useRef();
  const bagButtonRef = useRef();

  useEffect(() => { localStorage.setItem('zuffy-chat', JSON.stringify(messages.slice(-24))); }, [messages]);
  useEffect(() => { localStorage.setItem('zuffy-cart', JSON.stringify(cart)); }, [cart]);
  useEffect(() => { localStorage.setItem('zuffy-diet', JSON.stringify(diet)); localStorage.setItem('zuffy-avoid', JSON.stringify(avoid)); }, [diet, avoid]);
  useEffect(() => { if (messages.length || busy) endRef.current?.scrollIntoView({ behavior: 'smooth', block: 'nearest' }); }, [messages, busy]);
  useEffect(() => () => clearTimeout(toastTimer.current), []);

  async function refreshHealth() {
    try { setHealth(await api('/health', { timeout: 10000 })); setLoadError(''); }
    catch (error) { setHealth({ ollama: 'offline', message: error.message }); }
  }
  async function refreshOrders() {
    try { setOrders(await api(`/orders?sessionId=${encodeURIComponent(sessionId)}`)); setOrderError(''); }
    catch (error) { setOrderError(error.message); }
  }
  async function loadMenu() {
    setMenuLoading(true);
    try { setMenu(await api('/menu', { timeout: 10000 })); setLoadError(''); }
    catch (error) { setLoadError(error.message); }
    finally { setMenuLoading(false); }
  }
  useEffect(() => { loadMenu(); refreshHealth(); refreshOrders(); }, []);
  const hasActiveOrder = orders.some(order => order.status !== 'Delivered');
  useEffect(() => {
    if (tab !== 'orders' || !hasActiveOrder) return;
    const timer = setInterval(refreshOrders, 10000); return () => clearInterval(timer);
  }, [tab, hasActiveOrder]);

  useEffect(() => {
    if (!bagOpen) return;
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    bagRef.current?.querySelector('button')?.focus();
    function onKey(event) {
      if (event.key === 'Escape') { setBagOpen(false); return; }
      if (event.key !== 'Tab') return;
      const focusables = [...bagRef.current.querySelectorAll('button:not([disabled]), input, textarea, select, [tabindex="0"]')];
      const first = focusables[0], last = focusables.at(-1);
      if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last?.focus(); }
      else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first?.focus(); }
    }
    document.addEventListener('keydown', onKey);
    return () => { document.body.style.overflow = previousOverflow; document.removeEventListener('keydown', onKey); bagButtonRef.current?.focus(); };
  }, [bagOpen]);

  function notify(message) { setToast(message); clearTimeout(toastTimer.current); toastTimer.current = setTimeout(() => setToast(''), 2600); }
  function add(item) {
    if ((cart[item.id] || 0) >= 10) { notify('You can add up to 10 of each dish.'); return; }
    setCart(current => ({ ...current, [item.id]: (current[item.id] || 0) + 1 })); notify(`${item.name} added to your bag`);
  }
  function changeQuantity(id, delta) {
    setCart(current => {
      const next = { ...current }, quantity = (current[id] || 0) + delta;
      if (quantity <= 0) delete next[id]; else next[id] = Math.min(10, quantity);
      return next;
    });
  }
  const cartLines = menu.filter(item => cart[item.id]).map(item => ({ ...item, quantity: cart[item.id] }));
  const count = cartLines.reduce((total, item) => total + item.quantity, 0);
  const subtotal = cartLines.reduce((total, item) => total + item.price * item.quantity, 0);
  const deliveryFee = subtotal >= 399 || subtotal === 0 ? 0 : 29;

  async function send(text = input) {
    const value = text.trim();
    if (!value || sendLock.current) return;
    sendLock.current = true; setBusy(true); setChatError(''); setInput(''); setTab('assistant');
    const history = messages.filter(message => !message.localOnly).slice(-12).map(({ role, content }) => ({ role, content }));
    setMessages(current => [...current, { role: 'user', content: value }]);
    try {
      const response = await api('/chat', { method: 'POST', body: JSON.stringify({ message: value, sessionId, history, preferences: { diet, avoid } }) });
      setMessages(current => [...current, { role: 'assistant', content: response.reply, toolsUsed: response.toolsUsed, recommendations: response.recommendations }]);
      setHealth(current => ({ ...current, ollama: 'ready' }));
    } catch (error) { setChatError(error.message); setInput(value); }
    finally { sendLock.current = false; setBusy(false); textareaRef.current?.focus(); }
  }

  async function checkout() {
    if (!cartLines.length || checkoutBusy) return;
    setCheckoutBusy(true); setCheckoutError('');
    try {
      const order = await api('/orders', { method: 'POST', body: JSON.stringify({ sessionId, items: cartLines.map(item => ({ itemId: item.id, quantity: item.quantity })) }) });
      setOrders(current => [order, ...current]); setCart({}); setBagOpen(false); setTab('orders'); notify(`Demo order ${order.id} created`);
    } catch (error) { setCheckoutError(error.message); }
    finally { setCheckoutBusy(false); }
  }

  const filteredMenu = menu.filter(item => (category === 'All' || item.category === category) &&
    (diet === 'All' || item.diet === diet || (diet === 'Vegetarian' && item.diet === 'Vegan')) &&
    `${item.name} ${item.description} ${item.ingredients.join(' ')}`.toLowerCase().includes(search.toLowerCase()));
  const ready = health?.ollama === 'ready';

  return <div className="app-shell">
    <header className="header"><div className="header-inner">
      <button className="brand" onClick={() => setTab('assistant')} aria-label="Zuffy home"><span className="brand-icon">z<span>••</span></span><span>zuffy<span className="brand-dot">.</span></span></button>
      <nav aria-label="Main navigation">{[['assistant', 'Ask Zuffy'], ['menu', 'Explore menu'], ['orders', 'My orders']].map(([value, label]) => <button key={value} className={tab === value ? 'active' : ''} onClick={() => { setTab(value); if (value === 'orders') refreshOrders(); }}>{label}{value === 'assistant' && <span className="nav-ai">AI</span>}</button>)}</nav>
      <button ref={bagButtonRef} className="bag-button" onClick={() => setBagOpen(true)}><Icon name="bag" size={19}/><span>My bag</span><b>{count}</b></button>
    </div></header>

    <main>
      <div className="top-line"><span><i className="green-dot"/> A little hungry? You’re in the right place.</span><button className={`connection-pill ${ready ? 'connected' : ''}`} onClick={refreshHealth} title="Refresh assistant connection"><i/>{ready ? 'Assistant ready' : health ? 'Assistant offline' : 'Connecting…'}<Icon name="refresh" size={12}/></button></div>
      {(health && !ready) && <div className="notice" role="status"><Icon name="sparkle" size={19}/><span>{health.message || 'Start the Ollama app to chat. You can still explore the menu.'}</span><button onClick={refreshHealth}>Refresh</button></div>}

      {tab === 'assistant' && <>
        <section className="hero"><div><span className="eyebrow"><Icon name="sparkle" size={14}/> YOUR FOOD COMPANION</span><h1>Good food.<br/><span>Zero overthinking.</span></h1><p>A dinner idea, a quick recipe, or a little order help.<br className="desktop-break"/> Just ask Zuffy. We’ll figure it out together.</p></div><div className="hero-illustration" aria-hidden="true"><span className="orbit orbit-one"><FoodArt dish="avocado"/></span><span className="orbit orbit-two"><FoodArt dish="tomato"/></span><div className="hero-plate"><span><FoodArt dish="noodles"/></span><i className="plate-star">✦</i></div><span className="floating-note">made for your mood <span>↗</span></span><span className="hero-squiggle">✧</span></div></section>
        <div className="assistant-layout">
          <section className="chat-panel" aria-label="Chat with Zuffy">
            <div className="chat-header"><div className="assistant-avatar"><Icon name="sparkle" size={20}/></div><div><h2>Ask Zuffy <span>your delicious sidekick</span></h2><p>Recipes, recommendations & a little peace of mind.</p></div><button className="new-chat-button" onClick={() => { if (busy) return; setMessages([]); setChatError(''); setInput(''); }} disabled={busy} title="Clear this conversation" aria-label="New chat"><Icon name="plus" size={17}/><span>New chat</span></button></div>
            <div className={`chat-body ${messages.length ? 'has-messages' : ''}`} aria-live="polite" aria-busy={busy}>
              {!messages.length ? <div className="chat-welcome"><div className="welcome-symbol">✦</div><h3>Hey, hungry human.</h3><p>What sounds good today?</p><div className="suggestion-grid">{suggestions.map(suggestion => <button key={suggestion.title} onClick={() => send(suggestion.prompt)} disabled={busy}><span className="suggestion-emoji"><Icon name={suggestion.icon} size={22}/></span><span><strong>{suggestion.title}</strong><small>{suggestion.subtitle}</small></span><Icon name="arrow" size={15}/></button>)}</div></div> : messages.map((message, index) => <div key={index} className={`message ${message.role}`}>
                <div className="message-avatar">{message.role === 'assistant' ? <Icon name="sparkle" size={16}/> : 'You'}</div><div className="message-main"><span className="message-label">{message.role === 'assistant' ? 'Zuffy' : 'You'}</span><div className="message-content">{message.content}</div>{message.toolsUsed?.length > 0 && <div className="tool-chips">{message.toolsUsed.map(tool => <span key={tool}><Icon name="check" size={12}/>{tool}</span>)}</div>}{message.recommendations?.length > 0 && <div className="recommendations">{message.recommendations.map(item => <FoodCard key={item.id} item={item} onAdd={add} compact/>)}</div>}</div>
              </div>)}
              {busy && <div className="thinking"><span>✦</span><span className="dots"><i/><i/><i/></span><small>Zuffy is thinking…</small></div>}
              <div ref={endRef}/>
            </div>
            {chatError && <div className="chat-error" role="alert">{chatError} <button onClick={() => send(input)} disabled={busy}>Try again</button></div>}
            <form className="composer" onSubmit={event => { event.preventDefault(); send(); }}><div className="composer-box"><textarea ref={textareaRef} aria-label="Message Zuffy" placeholder="What are you craving? Ask me anything food…" value={input} maxLength={4000} rows={2} disabled={busy} onChange={event => setInput(event.target.value)} onKeyDown={event => { if (event.key === 'Enter' && !event.shiftKey && !event.nativeEvent.isComposing) { event.preventDefault(); send(); } }}/><button className="send-button" type="submit" disabled={!input.trim() || busy} aria-label="Send message"><Icon name="arrow" size={23}/></button></div><p><span>Thoughtful suggestions, powered by your local AI.</span><span>Enter to send ↵</span></p></form>
          </section>
          <aside className="side-panel"><div className="preference-card"><div className="section-heading"><Icon name="leaf" size={19}/><h2>Your kind of food</h2></div><p>A little about you goes a long way.</p><label htmlFor="diet">Diet preference</label><select id="diet" value={diet} onChange={event => setDiet(event.target.value)}><option>All</option><option>Vegetarian</option><option>Vegan</option><option>Non-Vegetarian</option></select><label htmlFor="avoid">Anything to avoid?</label><input id="avoid" placeholder="e.g. peanuts, mushrooms" value={avoid} maxLength={300} onChange={event => setAvoid(event.target.value)}/><div className="preference-foot"><Icon name="check" size={13}/> Remembered for this browser</div></div>
            <div className="inspiration-card"><span className="eyebrow">THE COMFORT CORNER</span><span className="inspiration-emoji"><FoodArt dish="dal-khichdi"/></span><h3>Some days call<br/>for a bowl of comfort.</h3><p>Let’s find your feel-good meal.</p><button onClick={() => send('Suggest a comforting meal from your menu that fits my preferences.')} disabled={busy}>Find my comfort food <Icon name="arrow" size={16}/></button></div>
            <div className="little-note"><span>✦</span><p>No big decisions.<br/>Just your next delicious one.</p></div>
          </aside>
        </div>
        <section className="favorites"><div className="section-title"><div><span className="eyebrow">A FEW CROWD FAVORITES</span><h2>Small cravings. Big happiness.</h2></div><button className="text-button" onClick={() => setTab('menu')}>Explore the menu <Icon name="arrow" size={17}/></button></div>{menuLoading ? <p className="loading-text">Finding something delicious…</p> : loadError ? <div className="notice" role="alert">{loadError}<button onClick={loadMenu}>Retry</button></div> : <div className="food-grid favorites-grid">{menu.slice(0, 3).map(item => <FoodCard key={item.id} item={item} onAdd={add}/>)}</div>}</section>
      </>}

      {tab === 'menu' && <section className="menu-section"><span className="eyebrow">FRESH PICKS, HAPPY PLATES</span><div className="page-title"><div><h1>Something delicious<br/><span>is waiting for you.</span></h1><p>A small menu, made for all your moods.</p></div><button className="outline-button" onClick={() => { setTab('assistant'); textareaRef.current?.focus(); }}><Icon name="sparkle" size={18}/> Help me choose</button></div><div className="menu-controls"><div className="category-tabs">{['All', 'Indian', 'Bowls', 'Pizza', 'Wraps', 'Desserts'].map(value => <button key={value} className={category === value ? 'selected' : ''} onClick={() => setCategory(value)}>{value}</button>)}</div><label className="search-box"><Icon name="search" size={17}/><input aria-label="Search menu" placeholder="Search a dish or ingredient" value={search} onChange={event => setSearch(event.target.value)}/></label></div><div className="menu-diet"><label htmlFor="menu-diet">Showing</label><select id="menu-diet" value={diet} onChange={event => setDiet(event.target.value)}><option>All</option><option>Vegetarian</option><option>Vegan</option><option>Non-Vegetarian</option></select><span>{filteredMenu.length} delicious choices</span></div>{menuLoading ? <p className="loading-text">Loading the menu…</p> : loadError ? <div className="notice" role="alert">{loadError}<button onClick={loadMenu}>Retry</button></div> : filteredMenu.length ? <div className="food-grid">{filteredMenu.map(item => <FoodCard key={item.id} item={item} onAdd={add}/>)}</div> : <div className="empty-state"><span><Icon name="leaf" size={55}/></span><h3>No dishes found</h3><p>Try a different search or dietary filter.</p><button className="outline-button" onClick={() => { setSearch(''); setDiet('All'); setCategory('All'); }}>Reset filters</button></div>}</section>}

      {tab === 'orders' && <section className="orders-section"><span className="eyebrow">FROM OUR KITCHEN TO YOUR SCREEN</span><div className="page-title"><div><h1>Your happy<br/><span>little orders.</span></h1><p>Demo orders, with a little real-time magic.</p></div><button className="outline-button" onClick={refreshOrders}><Icon name="refresh" size={17}/> Refresh orders</button></div>{orderError && <div className="notice" role="alert">{orderError}</div>}{orders.length ? <div className="orders-grid">{orders.map(order => <OrderCard key={order.id} order={order} onAsk={send}/>)}</div> : !orderError && <div className="empty-state"><span><Icon name="bag" size={55}/></span><h3>Your first craving is waiting.</h3><p>Add a dish to your bag and place a demo order.</p><button className="primary-button" onClick={() => setTab('menu')}>Explore menu <Icon name="arrow" size={17}/></button></div>}</section>}
    </main>

    <footer><span><strong>zuffy.</strong> A little food. A lot of feel-good.</span><span>Made with appetite <span className="orange-heart">♥</span> · Local demo</span></footer>
    {toast && <div className="toast" role="status"><Icon name="check" size={16}/>{toast}</div>}

    {bagOpen && <div className="bag-overlay" onClick={event => { if (event.target === event.currentTarget) setBagOpen(false); }}><aside className="bag-drawer" role="dialog" aria-modal="true" aria-labelledby="bag-title" ref={bagRef}><div className="bag-header"><div><span className="eyebrow">GOOD THINGS INSIDE</span><h2 id="bag-title">Your happy bag <span>({count})</span></h2></div><button className="icon-button" onClick={() => setBagOpen(false)} aria-label="Close bag"><Icon name="close"/></button></div>{cartLines.length ? <><div className="bag-lines">{cartLines.map(item => <div className="bag-line" key={item.id}><span className={`bag-food ${item.color}`}><FoodArt dish={item.id}/></span><div><h3>{item.name}</h3><strong>{money(item.price)}</strong><div className="quantity-control"><button onClick={() => changeQuantity(item.id, -1)} aria-label={`Remove one ${item.name}`}><Icon name="minus" size={13}/></button><span>{item.quantity}</span><button onClick={() => changeQuantity(item.id, 1)} disabled={item.quantity >= 10} aria-label={`Add one ${item.name}`}><Icon name="plus" size={13}/></button></div></div><strong>{money(item.price * item.quantity)}</strong></div>)}</div><div className="bag-summary"><div><span>Subtotal</span><span>{money(subtotal)}</span></div><div><span>Demo delivery fee</span><span>{deliveryFee ? money(deliveryFee) : 'Free'}</span></div>{subtotal < 399 && <p className="free-delivery-tip">Add {money(399 - subtotal)} more for free demo delivery.</p>}<div className="bag-total"><strong>Total</strong><strong>{money(subtotal + deliveryFee)}</strong></div>{checkoutError && <p className="checkout-error" role="alert">{checkoutError}</p>}<button className="primary-button checkout-button" onClick={checkout} disabled={checkoutBusy}>{checkoutBusy ? 'Creating your order…' : 'Place demo order'}<Icon name="arrow" size={18}/></button><p className="demo-note">This is a demo. No payment, restaurant dispatch, or real delivery.</p></div></> : <div className="empty-bag"><span><Icon name="bag" size={55}/></span><h3>A little empty, a lot of potential.</h3><p>Your next favorite dish belongs here.</p><button className="primary-button" onClick={() => { setBagOpen(false); setTab('menu'); }}>Find something delicious</button></div>}</aside></div>}
  </div>;
}
