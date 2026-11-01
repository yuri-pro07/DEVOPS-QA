param(
    [int]$Requisicoes = 500,
    [string]$Base = "http://localhost:8080"
)

$inicio = Get-Date
for ($i = 1; $i -le $Requisicoes; $i++) {
    $pagina = Get-Random -Minimum 0 -Maximum 5000
    Invoke-WebRequest -Uri "$Base/api/ranking?pagina=$pagina&tamanho=20" -UseBasicParsing | Out-Null
    if ($i % 10 -eq 0) {
        $aluno = "aluno-{0:D6}" -f (Get-Random -Minimum 1 -Maximum 100000)
        Invoke-WebRequest -Uri "$Base/api/ranking/alunos/$aluno" -UseBasicParsing | Out-Null
    }
    if ($i % 100 -eq 0) {
        Write-Host "$i requisicoes enviadas"
    }
}
$segundos = [math]::Round(((Get-Date) - $inicio).TotalSeconds, 1)
Write-Host "Concluido: $Requisicoes paginas do ranking em $segundos s"
