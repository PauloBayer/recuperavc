import './index.css'
import { useState } from 'react'

export default function Header() {
  const [menuOpen, setMenuOpen] = useState(false);

  const handleLinkClick = () => setMenuOpen(false);

  return (
    <nav className="header">
      <div className="menu-icon" onClick={() => setMenuOpen(!menuOpen)}>
        ☰
      </div>

      <ul className={menuOpen ? "nav-list active" : "nav-list"}>
        <li><a href="#home" onClick={handleLinkClick}>Home</a></li>
        <li><a href="#analises" onClick={handleLinkClick}>Sobre o Projeto</a></li>
        <li><a href="#oapp" onClick={handleLinkClick}>O App</a></li>
        <li><a href="#criadores" onClick={handleLinkClick}>Os Criadores</a></li>
      </ul>
    </nav>
  );
}
