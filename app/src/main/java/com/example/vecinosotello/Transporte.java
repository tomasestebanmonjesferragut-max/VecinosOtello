package com.example.vecinosotello;

/** Canal por el que se publican las alertas (MQTT en la app; un simulado en las pruebas). */
public interface Transporte {

    interface Resultado {
        void ok();

        void fallo(String motivo);
    }

    boolean conectado();

    void publicar(String topico, byte[] datos, Resultado resultado);
}
