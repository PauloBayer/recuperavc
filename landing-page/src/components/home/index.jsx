import './index.css'
import celularHome from '../assets/img/celular-home.png'

export default function Home(){
    return(
        <main id="home" className='home'>
            <div className='text-home'>
                <h1>RecuperAVC - Tecnologia a serviço da saúde pública</h1>
                <p>
                    Recuperar a fala. Retomar o movimento. Reconstruir a comunicação. 
                    O RecuperAVC é um aplicativo gratuito criado para ajudar pessoas que passaram por um AVC 
                    a treinar fala, coordenação e linguagem de forma simples e acessível.
                </p>

                <div className='btnContainer'>
                    <a 
                        href="https://1drv.ms/f/c/3a1476f72b82f7b5/IgBDUaHwRo2SRaE0pFFy1QEvAVAuYHx9dTg_kwuwo3_CiVY?e=T7ULbo" 
                        target="_blank" 
                        rel="noopener noreferrer" 
                        className='downloadButton'
                    >
                        📲 Baixar o App
                    </a>
                </div>
            </div>

            <img src={celularHome} alt="Tela inicial do aplicativo AnalisAVC" className='img-home' />
        </main>
    );
}
