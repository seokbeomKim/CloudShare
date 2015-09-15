package util;

import message.Message;
import message.MessageHandler;
import message.MessageReceiver;
import message.MessageSender;

abstract public class CSService extends Thread{
	protected MessageSender msg_sender;
	protected MessageReceiver msg_receiver;
	protected MessageHandler msg_handler;
	
	abstract public void run();
	abstract public void send(Message msg);
	abstract public void receive(Message msg);
	abstract public void handle(Message msg);
}
