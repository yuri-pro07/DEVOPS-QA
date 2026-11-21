from treinamento import precisa_treinar, treinar

if precisa_treinar():
    print("Treinando o modelo inicial:", treinar(), flush=True)
else:
    print("Modelo existente reaproveitado", flush=True)
