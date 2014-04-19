<?php

namespace OpenData\Irc;

use Symfony\Component\Console\Command\Command;
use Symfony\Component\Console\Input\InputArgument;
use Symfony\Component\Console\Input\InputInterface;
use Symfony\Component\Console\Input\InputDefinition;
use Symfony\Component\Console\Output\OutputInterface;
use Symfony\Component\Console\Input\ArgvInput;

class RunConsoleCommand extends Command
{
    private $bot;
    private $server = 'irc.esper.net';
    private $port = '5555';
    private $channels = array('OpenMods');
    private $nickname = 'OpenEye';
    private $app;
    private $admins = array();
    
    public function __construct($bot) {
        parent::__construct();
        
        $this->bot = $bot;
        $this->bot->setAutoExit(false);
        //$this->bot->setCatchExceptions(false);
        
        $this->bot->setDefinition(new InputDefinition(array(
            new InputArgument('command', InputArgument::REQUIRED, 'The command to execute'),
        )));
    }
    
    public function getAdmins() {
        return $this->admins;
    }
    
    protected function configure() {
        $this
            ->setName('bot:start')
            ->setDescription('Start the bot')
            ->addArgument(
                'channels',
                InputArgument::IS_ARRAY,
                'Channels',
                $this->channels
            )
        ;
    }

    protected function execute(InputInterface $input, OutputInterface $output) {
        
        $this->channels = $input->getArgument('channels');
        
        for ($i = 0; $i < count($this->channels); $i++) {
            $this->channels[$i] = '#'.$this->channels[$i];
        }
                
        $output->writeln('Starting bot');
        
        $regexServerCommand = "/^(?:[:@]([^\\s]+) )?([^\\s]+)(?: ((?:[^:\\s][^\\s]* ?)*))?(?: ?:(.*))?$/";
        $regexNickname = '/(?<=[^a-z_\-\[\]\\^{}|`])[a-z_\-\[\]\\^{}|`][a-z0-9_\-\[\]\\^{}|`]*/i'; // http://stackoverflow.com/a/5163309

        $this->socket = stream_socket_client($this->server.':'.$this->port);
        if (!is_resource($this->socket)) {
            throw new Exception('Unable to connect to server via fsockopen with server: "'.$this->server.'" and port: "'.$this->port.'".');
        }
        stream_set_timeout($this->socket, 2);
        $this->sendData('NICK '.$this->nickname);
        $this->sendData('USER '.$this->nickname.' 8 * :'.$this->nickname);
        
        $outputStream = new IrcOutputStream($this->socket);
        
        $expectingUserList = false;
        $outputStream->setBot($this);
        
        do {
            
            $data = fgets($this->socket);
            if ($data != '') {
                $data = preg_replace('/[\r\n]/', '', $data);
                preg_match($regexServerCommand, $data, $matches);
                
                echo $data."\n";
                if (isset($matches[2]) && ('' !== trim($matches[2]))) {
                    switch($matches[2]) {
                        
                        case 'PING':
                            $this->sendData('PONG '.$matches[4]);
                            $this->sendData('NAMES '.implode(', ', $this->channels));
                            break;
                        
                        case 'MODE':
                            foreach ($this->channels as $channel) {
                                $this->sendData('JOIN '.$channel);
                            }
                            break;
                            
                        case '353':
                            if (!$expectingUserList) {
                                $this->admins = array();
                                $expectingUserList = true;
                            }
                            foreach (explode(' ', $matches[4]) as $user) {
                                $modifier = substr($user, 0, 1);
                                if ($modifier == '@' || $modifier == '+') {
                                    $this->admins[] = substr($user, 1);
                                }
                            }
                            $this->admins = array_unique($this->admins);
                            break;
                        
                        case '353':
                                $expectingUserList = false;
                            break;
                            
                        case 'PRIVMSG':
                            $channel = trim($matches[3]);
                            if (preg_match($regexNickname, $matches[0], $nameMatches)) {
                                if (isset($nameMatches[0])) {
                                    $user = $nameMatches[0];
                                }
                            }
                            
                            $outputStream->setTarget(
                                $channel == $this->nickname ?
                                IrcOutputStream::USER :
                                IrcOutputStream::CHANNEL
                            );
                            
                            $outputStream->setChannel($channel);
                            $outputStream->setUser($user);
                            $message = explode(' ', trim($matches[4]));
                            
                            if (count($message) > 0 && substr($message[0], 0, 1) == '!') {
                                
                                array_unshift($message, '');
                                
                                $message[1] = substr($message[1], 1);
                                if($this->bot->has($message[1])) {
                                    try {
                                        $this->bot->run(new ArgvInput($message), $outputStream);
                                    } catch(\Exception $e) {
                                        
                                    }
                                }
                            }
                            break;
                    }
                }
            }
            
        } while (true);
    }
    
    private function sendData($data) {
        echo 'Sending '.$data."\n";
        return fwrite($this->socket, $data . "\n");
    }
}
