import io, os, re, sys

RAIZ = 'br.edu.gamificacaocursos'


def pacote_de(caminho, base):
    rel = os.path.relpath(caminho, base).replace('\\', '/')
    pkg = os.path.dirname(rel).replace('/', '.')
    return pkg or '(raiz)'


def coletar(base):
    arquivos = []
    for dirpath, _, nomes in os.walk(base):
        for n in nomes:
            if n.endswith('.java'):
                arquivos.append(os.path.join(dirpath, n))
    return sorted(arquivos)


def analisar(base):
    arestas = {}
    pacotes = set()
    for arq in coletar(base):
        pkg = pacote_de(arq, base)
        pacotes.add(pkg)
        texto = io.open(arq, encoding='utf-8').read()
        for imp in re.findall(r'^import\s+(?:static\s+)?([\w.]+);', texto, re.M):
            if not imp.startswith(RAIZ):
                continue
            destino = imp[len(RAIZ):].lstrip('.')
            destino = '.'.join(destino.split('.')[:-1]) or '(raiz)'
            if destino != pkg:
                arestas.setdefault(pkg, {}).setdefault(destino, 0)
                arestas[pkg][destino] += 1
    return arestas, pacotes


def relatorio(base, titulo):
    arestas, pacotes = analisar(base)
    linhas = [titulo, '=' * len(titulo), '']
    linhas.append('Pacotes de primeiro nivel: %d' % len({p.split('.')[0] for p in pacotes}))
    linhas.append('Pacotes no total: %d' % len(pacotes))
    linhas.append('')
    linhas.append('Dependencias entre pacotes (origem -> destino: nro de imports):')
    total = 0
    cruzam_topo = 0
    for origem in sorted(arestas):
        for destino in sorted(arestas[origem]):
            n = arestas[origem][destino]
            total += n
            if origem.split('.')[0] != destino.split('.')[0]:
                cruzam_topo += n
            linhas.append('  %-28s -> %-28s %d' % (origem, destino, n))
    linhas.append('')
    linhas.append('Total de dependencias entre pacotes: %d' % total)
    linhas.append('Dependencias que cruzam o primeiro nivel: %d' % cruzam_topo)
    return '\n'.join(linhas)


if __name__ == '__main__':
    sys.stdout.reconfigure(encoding='utf-8')
    print(relatorio(sys.argv[1], sys.argv[2]))
