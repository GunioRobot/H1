<?php

$GLOBALS['THRIFT_ROOT'] = './thrift_php/src';

require_once $GLOBALS['THRIFT_ROOT'].'/Thrift.php';
require_once $GLOBALS['THRIFT_ROOT'].'/protocol/TBinaryProtocol.php';
require_once $GLOBALS['THRIFT_ROOT'].'/transport/TSocket.php';
require_once $GLOBALS['THRIFT_ROOT'].'/transport/TMemoryBuffer.php';
require_once $GLOBALS['THRIFT_ROOT'].'/transport/TBufferedTransport.php';

error_reporting(E_ALL);
$GEN_DIR = '.';
require_once $GEN_DIR.'/Protocol.php';
error_reporting(E_ALL);

try {
  $target = $_GET['target'];
  $port = $_GET['port'];
  $colour = $_GET['colour'];
  
  $socket = new TSocket($target, $port);
  $transport = new TBufferedTransport($socket, 1024, 1024);
  $protocol = new TBinaryProtocol($transport);
  $client = new ProtocolClient($protocol);
  $transport->open();

  $message = build_message($colour);
  $message = sign_message($message);
  $client->updateAttributes($message);
  $transport->close();

  header("HTTP/1.0 " . 200 . " Ok");
}catch (Exception $e){
  header("HTTP/1.0 " . 500 . " Internal Server Error");
  echo($e);
}

function sign_message($message) {
  $message_bin = get_message_bytes($message);
  $md5 = md5($message_bin, true);    
  $message->messageBase->checksum = $md5;
  return $message;
}

function build_message($colour) {
  $originator = new Peer();
  $originator->host = 'client.com';
  $originator->port = 9797;
  $messageBase = new MessageBase();
  $messageBase->originator = $originator;

  $message = new UpdateAttributesMessage();
  $message->messageBase = $messageBase;
  $message->attributes = array();
  $message->attributes['test.prop.1'] = $colour;
  $message->messageId = time();
  return $message;
}

function get_message_bytes($message){
  $mem = new TMemoryBuffer();
  $transport = new TBufferedTransport($mem, 1024, 1024);
  $protocol = new TBinaryProtocol($transport);
  $transport->open();
  $message->write($protocol);
  $transport->flush();
  return $mem->getBuffer();
}


?>
