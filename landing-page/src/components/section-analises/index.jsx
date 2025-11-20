import './index.css'
import avoice from "../assets/img/Avoice.png"
import atapping from "../assets/img/Atapping.png"
import awrite from "../assets/img/Awrite.png"

export default function SectionAnalises(){
    return(
        <section id="oapp" className='section'>
            <h2>Acompanhe seu desempenho</h2>
            <div className='imgAnalises'>
                <img src={avoice} alt="Análise de fala"/>
                <img src={atapping} alt="Análise de coordenação motora"/>
                <img src={awrite} alt="Análise de escrita"/>
            </div>
            <div className='btnContainer'>
                <a 
                    href="https://1drv.ms/f/c/3a1476f72b82f7b5/IgDwPUHl6Us5RZVTGmhIuK6eAVXCZngPpSxnXuOPVYMkIyk?e=OY5ru4" 
                    target="_blank" 
                    rel="noopener noreferrer"
                    className="downloadButton"
                >
                    Baixar artigo científico
                </a>
            </div>
        </section>
    )
}