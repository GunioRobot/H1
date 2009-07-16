<?php

$url = $_GET['target'];
$session = curl_init($url);
curl_setopt($session, CURLOPT_HEADER, false);
curl_setopt($session, CURLOPT_RETURNTRANSFER, true);
$xml = curl_exec($session);
$httpcode = curl_getinfo($session, CURLINFO_HTTP_CODE);
header("HTTP/1.0 " . $httpcode);
echo $xml;
curl_close($session);

?>
