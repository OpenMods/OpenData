<?php


namespace OpenData\Irc;

use Symfony\Component\Console\Output\StreamOutput;

class IrcOutputStream extends StreamOutput {
    
    private $user;
    private $bot;

    public function setUser($user) {
        $this->user = $user;
    }
    
    public function getUser() {
        return $this->user;
    }
    
    public function setBot($bot) {
        $this->bot = $bot;
    }
    
    public function getBot() {
        return $this->bot;
    }
    
    protected function doWrite($message, $newline) {
        
        $newline = true;
        $message = trim($message);
        
        if (empty($message)) return;

        $message = 'NOTICE '.$this->getUser().' :'.$message;
        echo "Sending ".$message."\n";
        
        if (false === @fwrite($this->getStream(), $message.($newline ? PHP_EOL : ''))) {
            throw new \RuntimeException('Unable to write output.');
        }

        fflush($this->getStream());  
    }
    
}
