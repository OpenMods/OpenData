<?php


namespace OpenData\Irc;

use Symfony\Component\Console\Output\StreamOutput;

class IrcOutputStream extends StreamOutput {
    
    const CHANNEL = 0;
    const USER = 1;
    
    private $channel;
    private $user;
    private $target;
    private $bot;
    
    public function setChannel($channel) {
        $this->channel = $channel;
    }
    
    public function setUser($user) {
        $this->user = $user;
    }
    
    public function getUser() {
        return $this->user;
    }
    
    public function setTarget($target) {
        $this->target = $target;
    }
    
    public function getTarget() {
        return $this->target;
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
        
        $target = $this->channel;
        if ($this->target == self::USER) {
            $target = $this->user;
        }
        
        
        $message = 'PRIVMSG '.$target.' :'.$message;
        echo "Sending ".$message."\n";
        
        if (false === @fwrite($this->getStream(), $message.($newline ? PHP_EOL : ''))) {
            throw new \RuntimeException('Unable to write output.');
        }

        fflush($this->getStream());  
    }
    
}
