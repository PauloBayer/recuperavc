import './index.css'
import bayer from '../assets/img/bayer.jpg'
import fellipe from '../assets/img/fellipe.jpg'
import nina from '../assets/img/nina.jpg'

export default function SectionCriadores(){
    const autores = [
        {
        nome: 'Fellipe Gabriel Gonçalves de Araújo',
        funcao: 'Desenvolvedor',
        linkedin: 'https://www.linkedin.com/in/gabrielfellipe/',
        foto: fellipe,
        },
        {
        nome: 'Janaina Fonseca Nogueira',
        funcao: 'Desenvolvedora',
        linkedin: 'https://www.linkedin.com/in/janaina-nogueira-926368181/',
        foto: nina,
        },
        {
        nome: 'Paulo Eduardo Bayer Kresko',
        funcao: 'Desenvolvedor',
        linkedin: 'https://www.linkedin.com/in/paulo-bayer/',
        foto: bayer,
        },
    ];

    return(
        <section id="criadores" className="section-creators">
            <div className="section-creators-header">
                <h2>Os Criadores</h2>
                <p>
                Três indivíduos, um objetivo em comum: aproximar a tecnologia da saúde pública
                para ajudar na reabilitação de quem passou por um AVC. 
                </p>
            </div>

            <div className="creators-grid">
                {autores.map((autor) => (
                <article className="creator-card" key={autor.nome}>
                    <div className="creator-avatar-wrapper">
                    <img
                        src={autor.foto}
                        alt={autor.nome}
                        className="creator-avatar"
                    />
                    </div>

                    <h3 className="creator-name">{autor.nome}</h3>

                    {autor.funcao && (
                    <p className="creator-role">{autor.funcao}</p>
                    )}

                    <a
                    href={autor.linkedin}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="creator-link"
                    >
                    <span>Ver perfil no LinkedIn</span>
                    <span className="creator-link-icon">↗</span>
                    </a>
                </article>
                ))}
            </div>
        </section>
    )
}