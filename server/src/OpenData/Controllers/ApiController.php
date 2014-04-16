<?php

namespace OpenData\Controllers;

use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\Request;
use JsonSchema\Uri\UriRetriever;
use JsonSchema\Validator;
use OpenData\PacketHandlers\IPacketHandler;

class ApiController {

    private static $FLOOD_LIMIT = 10000; // during dev
    
    protected $memcache;
    
    private $schemas = array();
    
    /**
     *
     * @var type IPacketHandler[]
     */
    private $packetHandlers = array();

    public function __construct($memcache) {
        $this->memcache = $memcache;
    }

    public function registerPacketHandler(IPacketHandler $handler) {
        
        $this->packetHandlers[$handler->getPacketType()] = $handler;
        
        $schema = $handler->getJsonSchema();
        
        if (!isset($this->schemas[$schema])) {
            $retriever = new UriRetriever();
            $this->schemas[$schema] = $retriever->retrieve('file://' . __DIR__ . '/../Schemas/'.$schema);
        }
        
    }
    
    public function main(Request $request) {

        $content = $request->getContent();
        
        if (0 === strpos($request->headers->get('Content-Encoding'), 'gzip')) {
            $content = gzinflate(substr($content, 10, -8));
        }
        
        if ($this->isUserFlooding($request)) {
            return new JsonResponse(array());
        }

        $data = json_decode(mb_convert_encoding($content, 'UTF-8', 'auto'), true);

        if (!is_array($data)) {
            throw new \Exception('Array expected');
        }

        $responses = array();

        foreach ($data as $packet) {

            if (!isset($packet['type']) || !is_string($packet['type'])) {
                throw new \Exception('Packet type not defined');
            }
            
            $type = $packet['type'];

            $response = null;

            if (!isset($this->packetHandlers[$type])) {
                throw new \Exception('Invalid packet type '.$type);
            }
            
            $handler = $this->packetHandlers[$type];

            $errors = $this->getErrors($packet, $handler->getJsonSchema());

            unset($packet['type']);

            if ($errors != null) {
                throw new \Exception(implode("\n", $errors));
            }

            if ($response = $handler->execute($packet)) {
                $responses = array_merge($responses, $response);
            }
        }

        return new JsonResponse($responses);
    }
    
    private function isUserFlooding(Request $request) {

        if ($this->memcache == null)
            return false;

        $key = sha1($request->getClientIp() . date('Y-m-d-H'));
        $requestCount = $this->memcache->get($key);

        if ($requestCount) {
            if ($requestCount > self::$FLOOD_LIMIT) {
                return true;
            }
            $this->memcache->replace($key, $requestCount + 1, 0, 3600);
        } else {
            $this->memcache->set($key, 1, 0, 3600);
        }

        return false;
    }

    private function getErrors($packet, $schema) {

        // real nasty, but the json validator requires we pass in as an stdClass.
        // so we'll recode it as a class.
        $packet = json_decode(json_encode($packet), false);

        $validator = new Validator();
        $validator->check($packet, $this->schemas[$schema]);
        
        if (!$validator->isValid()) {
            $errors = array();
            foreach ($validator->getErrors() as $error) {
                $errors[] = sprintf("[%s] %s\n", $error['property'], $error['message']);
            }
            return $errors;
        }

        return null;
    }

}
