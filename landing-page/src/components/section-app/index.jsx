import './index.css';

import quotes from '../assets/icon/quotes.png';
import tap from '../assets/icon/tap.png';
import voice from '../assets/icon/voice-search.png';

export default function SectionApp() {
  const cards = [
    {
      img: quotes,
      title: 'FORMAÇÃO DE FRASES',
      text: 'Estimula raciocínio linguístico e memória por meio da montagem de sentenças com palavras embaralhadas.',
    },
    {
      img: tap,
      title: 'COORDENAÇÃO MOTORA',
      text: 'Propõe testes de cliques em pontos fixos e móveis para avaliar tempo de resposta e precisão.',
    },
    {
      img: voice,
      title: 'FALA',
      text: 'Utiliza Inteligência Artificial para reconhecer a frase dita pelo usuário, medir a porcentagem de acerto e a velocidade da fala.',
    },
  ];

  return (
    <section id="analises" className="section-app">
      <div className="card-container">
        {cards.map((card, index) => (
          <div className="card" key={index}>
            <img src={card.img} alt={card.title} />
            <div className="card-content">
              <h2>{card.title}</h2>
              <p>{card.text}</p>
            </div>
          </div>
        ))}
      </div>

      <div className="text">
        <p>
          O AVC é uma das principais causas de incapacidade funcional no Brasil.
          Entre as sequelas mais comuns estão as dificuldades de fala, compreensão
          e coordenação motora fina. Pensando nisso, o <strong>AnalisAVC</strong> foi
          criado para oferecer uma ferramenta tecnológica complementar ao processo
          de reabilitação, especialmente voltada a pacientes, familiares e
          profissionais da saúde.
        </p>
        <p>
          O aplicativo reúne três módulos principais de treinamento e análise. A
          aplicação foi desenvolvida em plataforma Android, com interface adaptada
          para idosos e pessoas com limitações motoras.
        </p>
      </div>
    </section>
  );
}
