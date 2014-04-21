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
    private $channels = array('OpenEye');
    private $nickname = 'OpenEye';
    private $app;
    private $admins = array();
    private $voiced = array();
    private $accounts = array();
    
    public function __construct($bot) {
        parent::__construct();
        
        $this->bot = $bot;
        $this->bot->setAutoExit(false);
        $this->bot->setCatchExceptions(false);
        
        $this->bot->setDefinition(new InputDefinition(array(
            new InputArgument('command', InputArgument::REQUIRED, 'The command to execute'),
        )));
    }
    
    public function getAdmins() {
        return $this->admins;
    }
    
    public function getVoiced() {
        return $this->voiced;
    }
    
    public function getUsername($nickname) {
        return isset($this->accounts[$nickname]) ?
        $this->accounts[$nickname] : null;
    }
    
    protected function configure() {
        $this
            ->setName('bot:start')
            ->setDescription('Start the bot')
            ->addArgument(
                'password',
                InputArgument::REQUIRED,
                'Password'
            )
            ->addArgument(
                'channels',
                InputArgument::IS_ARRAY,
                'Channels',
                array()
            )
        ;
    }

    protected function execute(InputInterface $input, OutputInterface $output) {
        
        $this->channels = $input->getArgument('channels');
        $this->channels[] = 'OpenEye';
        
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
                            break;
                        
                        case 'MODE':
                            
                            if (trim($matches[3]) == 'OpenEye') {
                                $this->sendData('PRIVMSG nickserv :identify '.$this->nickname.' '.$input->getArgument('password'));
                                foreach ($this->channels as $channel) {
                                    $this->sendData('JOIN '.$channel);
                                }
                            } else {
                                                                
                                if (preg_match("@\+([o|v]+)\s(.*)$@", $matches[3], $parts)) {
                                    
                                    if ($parts[1] == 'o' || $parts[1] == 'v') {
                                        
                                        if ($parts[1] == 'o') {
                                            $this->admins[] = $parts[2];
                                        } else if ($parts[1] == 'v') {
                                            $this->voiced[] = $parts[2];
                                        }
                                        
                                        $this->admins = array_unique($this->admins);
                                        $this->voiced = array_unique($this->voiced);
                                    
                                        $this->sendData('WHOIS '.$parts[2]);
                                    
                                    }
                                }
                            }
                            
                            break;
                            
                        case '353':
                            if ($matches[3] == 'OpenEye = #OpenEye ') {
                                
                                foreach (explode(' ', $matches[4]) as $user) {
                                    $modifier = substr($user, 0, 1);
                                    if ($modifier == '@') {
                                        $username = substr($user, 1);
                                        
                                        $this->admins[] = $username;
                                        $this->sendData('WHOIS '.$username);
                                        
                                    } else if ($modifier == '+') {
                                        $username = substr($user, 1);
                                        
                                        $this->voiced[] = $username;
                                        $this->sendData('WHOIS '.$username);
                                    }
                                }
                                
                            }
                            break;
                        case 'PART':
                            
                            preg_match($regexNickname, $data, $outUser);
                            
                            $nickname = trim($outUser[0]);
                            
                            var_dump($nickname);
                            
                            if(($key = array_search($nickname, $this->admins)) !== false) {
                                unset($this->admins[$key]);
                            }
                            if(($key = array_search($nickname, $this->voiced)) !== false) {
                                unset($this->voiced[$key]);
                            }
                            unset($this->accounts[$nickname]);
                                                        
                            break;
                            
                        case 'NICK':
                            
                            preg_match($regexNickname, $data, $outUser);
                            
                            $oldName = trim($outUser[0]);
                            $newName = $matches[4];
                            
                            for ($i = 0; $i < count($this->admins); $i++) {
                                if ($this->admins[$i] == $oldName) {
                                    $this->admins[$i] = $newName;
                                }
                            }
                            for ($i = 0; $i < count($this->voiced); $i++) {
                                if ($this->voiced[$i] == $oldName) {
                                    $this->voiced[$i] = $newName;
                                }
                            }
                            
                            $oldAcc = $this->accounts[$oldName];
                            unset($this->accounts[$oldName]);
                            $this->accounts[$newName] = $oldAcc;
                            
                            break;
                        
                        case '330':
                            
                            preg_match('@^(.*)\s(.*)\s(.*)\s$@', $matches[3], $userParts);
                            
                            $nick = $userParts[2];
                            $acc = $userParts[3];
                            
                            $this->accounts[$nick] = $acc;
                            
                            break;
                            
                        case 'PRIVMSG':
                            $channel = trim($matches[3]);
                            if (preg_match($regexNickname, $matches[0], $nameMatches)) {
                                if (isset($nameMatches[0])) {
                                    $user = $nameMatches[0];
                                }
                            }
                            $outputStream->setChannel($channel);
                            $outputStream->setUser($user);
                            $outputStream->setOutputType(IrcOutputStream::OUTPUT_PRIVMSG);
                            preg_match_all('#(?<!\\\\)("|\')(?<escaped>(?:[^\\\\]|\\\\.)*?)\1|(?<unescaped>\S+)#s', trim($matches[4]), $args, PREG_SET_ORDER);
                            
                            $message = array();
                            foreach($args as $arr){
                               if(!empty($arr['escaped'])){
                                  $message[] = $arr['escaped'];
                               }else{
                                  $message[] = $arr['unescaped'];
                               }
                            }
                                                        
                            if (count($message) > 0 && substr($message[0], 0, 1) == '!') {
                                
                                array_unshift($message, '');
                                
                                $message[1] = substr($message[1], 1);
                                $superNamespace = current(explode(':', $message[1]));
                                
                                try {
                                    
                                    if($this->bot->findNamespace($superNamespace)) {
                                        try {
                                            $this->bot->run(new ArgvInput($message), $outputStream);
                                        } catch(\Exception $e) {
                                            $outputStream->write('Command not found. Sending help as notice');
                                            
                                            $outputStream->setOutputType(IrcOutputStream::OUTPUT_NOTICE);
                                            $list = array();
                                            foreach ($this->bot->all() as $cmd => $c) {
                                                $list[] = $cmd;
                                            }
                                            foreach (array_chunk($list, 10) as $chunk) {
                                                $outputStream->write(implode(', ', $chunk));
                                            }
                                        }
                                    }
                                }catch (\Exception $e) {
                                    $outputStream->write('Command not found');
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
