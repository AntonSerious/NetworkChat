package ru.geekbrains.java_two.network;

import java.net.Socket;

public interface SocketThreadListener {

    void onSocketStart(SocketThread thread, Socket socket);
    void onSocketStop(SocketThread thread);

    void onSocketReady(SocketThread thread, Socket socket);
    void onReceiveString(SocketThread thread, Socket socket, String msg);

    void onSocketException(SocketThread thread, Exception exception);
}

/*
У меня куча вопросов и одновременно ни одного по вашему коду, так как я долго в нём копался и исправляя свои ошибки всё уже облазил и принцип понял досканально, но одновременно временами я забываю как он вообще работает. Наверное надо просто переварить, на мой взгляд это очень тяжёлая для понимания, хотя и очень красивая реализация.

1. Куда "отплывают" сокеты после создания?
2. класс SimpleClient. try(Socket socket = new Socket("127.0.0.1", 8189)){ // Правильно понимаю, что здесь создание Сокета тоже блокирует процесс на какое то время? По аналогии с accept.
                        System.out.println("client connected"); ....
                        }
3. На 2 часу 40 минуте звучит фраза - "сокет работает со стороны клиента и со стороны сервера". Идет речь про SocketThread?
  Получается, у нас есть ServerSocketThread, который порождает сокет.
  А еще есть SocketThread, который делает треды по родившемуся сокету как на стороне сервера, так и треды на стороне клиента?
4. Какой смысл у события onSocketReady?
5. на 3 часу 01 минуте - не понял, почему происходит SendMessage, пока "достаем" стримы?

не уяснил, как сервер принимает из стрима сообщения от клиентов?

Для меня очень тяжело понимание, как между собой взаимодействуют объекты. Долго ломал голову, что происходит в этом куске кода
listener.onSocketStart(this, socket);
DataInputStream in = new DataInputStream(socket.getInputStream());
out = new DataOutputStream(socket.getOutputStream());
listener.onSocketReady(this, socket);

А собственно, как правильно начинать разбор кода? Ну вот дали задание «разобраться с кодом», как понять откуда стоит начинать? На примере модуля network, откуда бы вы начали и как бы просматривали подобный код?

Подробно рассказать про сущности -  thread и socket  и их взаимодействия, жизненные циклы этих товарищей.

* */