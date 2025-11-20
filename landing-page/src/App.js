import './App.css';
import Header from './components/header'
import Home from './components/home'
import SectionAnalises from './components/section-analises';
import SectionApp from './components/section-app'
import SectionCriadores from './components/section-criadores'

function App() {
  return (
    <div className="App">
      <Header/>
      <Home/>
      <SectionApp/>
      <SectionAnalises/>
      <SectionCriadores/>
    </div>
  );
}

export default App;
