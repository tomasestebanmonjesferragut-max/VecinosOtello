"""
Prueba de integración de las reglas de Realtime Database contra el proyecto real (vecinosotello).

Uso:  python reglas.py <archivo-de-claves>
El archivo de claves NO está en el repositorio. Cada línea tiene la forma:
  Casa 3538:  otello3538sanjoaquin@gmail.com  /  clave: XXXXXXXX
La casa 3538 debe ser administrador y la 3530 vecino.

Cada caso imprime: id, resultado esperado, resultado obtenido y estado (OK / FALLA).
Los datos de prueba que se crean se borran al final.
"""
import json
import re
import sys
import time
import urllib.error
import urllib.request

PROYECTO = "vecinosotello"
BASE = "https://vecinosotello-default-rtdb.firebaseio.com"
CLAVE_API = json.load(open("../app/google-services.json", encoding="utf-8"))["client"][0]["api_key"][0]["current_key"]


def pedir(metodo, url, cuerpo=None):
    datos = None if cuerpo is None else json.dumps(cuerpo).encode()
    req = urllib.request.Request(url, data=datos, method=metodo, headers={"Content-Type": "application/json"})
    try:
        with urllib.request.urlopen(req, timeout=30) as r:
            return r.status, json.loads(r.read() or "null")
    except urllib.error.HTTPError as e:
        return e.code, None


def entrar(correo, clave):
    url = "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=" + CLAVE_API
    estado, r = pedir("POST", url, {"email": correo, "password": clave, "returnSecureToken": True})
    if estado != 200 or r is None:
        sys.exit(f"No se pudo iniciar sesión con {correo} (HTTP {estado})")
    return r["idToken"], r["localId"]


def ruta(path, token=None):
    return f"{BASE}/{path}.json" + (f"?auth={token}" if token else "")


resultados = []


def caso(id_, descripcion, esperado_ok, estado):
    obtenido_ok = estado == 200
    resultados.append(obtenido_ok == esperado_ok)
    print(f"{id_}  {descripcion}\n      esperado: {'permitido' if esperado_ok else 'rechazado'}"
          f" | obtenido: {'permitido' if obtenido_ok else 'rechazado'} (HTTP {estado})"
          f" | {'OK' if obtenido_ok == esperado_ok else 'FALLA'}")


def main():
    if hasattr(sys.stdout, "reconfigure"):
        getattr(sys.stdout, "reconfigure")(encoding="utf-8")  # acentos correctos en la consola de Windows
    texto = open(sys.argv[1], encoding="utf-8").read()
    cuentas = {num: (correo, clave) for num, correo, clave in
               re.findall(r"Casa (\w+):\s+(\S+)\s+/\s+clave: (\S+)", texto)}
    t_admin, uid_admin = entrar(*cuentas["3538"])
    t_vecino, uid_vecino = entrar(*cuentas["3530"])
    ahora = {".sv": "timestamp"}

    caso("R01", "Sin sesión no se puede leer el chat", False, pedir("GET", ruta("mensajes"))[0])
    caso("R02", "Vecino con sesión lee el chat", True, pedir("GET", ruta("mensajes", t_vecino) + "&limitToLast=1&orderBy=%22fecha%22")[0])

    anuncio = {"titulo": "Prueba de reglas", "contenido": "Se borra al terminar", "autor": "Prueba",
               "uid": uid_vecino, "importante": False, "fecha": ahora}
    caso("R03", "Vecino intenta publicar un anuncio (saltándose la app)", False,
         pedir("PUT", ruta("anuncios/prueba-vecino", t_vecino), anuncio)[0])
    anuncio["uid"] = uid_admin
    caso("R04", "Administrador publica un anuncio", True, pedir("PUT", ruta("anuncios/prueba-admin", t_admin), anuncio)[0])

    caso("R05", "Vecino intenta cambiar su rol a admin", False,
         pedir("PUT", ruta(f"usuarios/{uid_vecino}/rol", t_vecino), "admin")[0])
    caso("R06", "Vecino intenta cambiar el rol de otro usuario", False,
         pedir("PUT", ruta(f"usuarios/{uid_admin}/rol", t_vecino), "vecino")[0])

    perfil = json.loads(urllib.request.urlopen(ruta(f"usuarios/{uid_vecino}", t_vecino)).read())
    msg = {"uid": uid_vecino, "nombre": "Otro nombre", "vivienda": perfil["vivienda"], "texto": "hola", "fecha": ahora}
    caso("R07", "Mensaje con un nombre distinto al del perfil (suplantación)", False,
         pedir("PUT", ruta("mensajes/prueba-falso", t_vecino), msg)[0])
    msg["uid"] = uid_admin
    msg["nombre"] = perfil["nombre"]
    caso("R08", "Mensaje firmado con el uid de otro usuario", False,
         pedir("PUT", ruta("mensajes/prueba-falso2", t_vecino), msg)[0])

    alerta = {"id": "prueba-alerta", "comunidad": "otello", "uid": uid_vecino, "nombre": perfil["nombre"],
              "vivienda": perfil["vivienda"], "tipo": "Necesito ayuda", "fecha": int(time.time() * 1000), "estado": "activa"}
    caso("R09", "Vecino registra su alerta (estado activa)", True,
         pedir("PUT", ruta("alertas/prueba-alerta", t_vecino), alerta)[0])
    caso("R10", "Vecino intenta marcar la alerta como cerrada", False,
         pedir("PUT", ruta("alertas/prueba-alerta/estado", t_vecino), "cerrada")[0])
    caso("R11", "Vecino confirma que vio la alerta", True,
         pedir("PUT", ruta(f"alertas/prueba-alerta/vistos/{uid_vecino}", t_vecino), ahora)[0])
    caso("R12", "Administrador marca la alerta como atendida", True,
         pedir("PATCH", ruta("alertas/prueba-alerta", t_admin), {"estado": "atendida", "atendidaPor": "Prueba"})[0])
    caso("R13", "Estado inválido de alerta", False,
         pedir("PUT", ruta("alertas/prueba-alerta/estado", t_admin), "borrada")[0])

    venta = {"uid": uid_vecino, "nombre": perfil["nombre"], "vivienda": perfil["vivienda"], "titulo": "Prueba",
             "descripcion": "", "categoria": "Comida", "precio": 2500, "vendido": False, "fecha": ahora}
    caso("R14", "Vecino publica un producto válido", True, pedir("PUT", ruta("ventas/prueba-venta", t_vecino), venta)[0])
    caso("R15", "Otro usuario intenta marcar ese producto como vendido", False,
         pedir("PUT", ruta("ventas/prueba-venta/vendido", t_admin), True)[0])
    caso("R16", "El dueño lo marca como vendido", True,
         pedir("PUT", ruta("ventas/prueba-venta/vendido", t_vecino), True)[0])
    venta["precio"] = 12.5
    caso("R17", "Producto con precio con decimales", False, pedir("PUT", ruta("ventas/prueba-venta2", t_vecino), venta)[0])

    # Limpieza (el admin puede borrar anuncios y alertas; el dueño su producto)
    pedir("DELETE", ruta("anuncios/prueba-admin", t_admin))
    pedir("DELETE", ruta("alertas/prueba-alerta", t_admin))
    pedir("DELETE", ruta("ventas/prueba-venta", t_vecino))
    print(f"\n{sum(resultados)} de {len(resultados)} casos OK")
    sys.exit(0 if all(resultados) else 1)


if __name__ == "__main__":
    main()
