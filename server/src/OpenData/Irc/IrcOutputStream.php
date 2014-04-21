<?php


namespace OpenData\Irc;

use Symfony\Component\Console\Output\StreamOutput;

class IrcOutputStream extends StreamOutput {
    
    const OUTPUT_NOTICE = 0;
    const OUTPUT_PRIVMSG = 1;
    
    private $user;
    private $bot;
    private $channel;
    private $type;

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
    
    public function setChannel($channel) {
        $this->channel = $channel;
    }
    
    public function setOutputType($type) {
        $this->type = $type;
    }
    
    protected function doWrite($message, $newline) {
        
        $newline = true;
        $message = trim($message);
        
        if (empty($message)) return;
        
        
        $target = $this->getUser();
        $msgType = 'NOTICE';
        
        if ($this->type == self::OUTPUT_PRIVMSG) {
            $target = $this->channel;
            $msgType = 'PRIVMSG';
        }

        $message = $msgType.' '.$target.' :'.$message;
        
        echo "Sending ".$message."\n";
        
        if (false === @fwrite($this->getStream(), $message.($newline ? PHP_EOL : ''))) {
            throw new \RuntimeException('Unable to write output.');
        }

        fflush($this->getStream());  
    }
    
}
