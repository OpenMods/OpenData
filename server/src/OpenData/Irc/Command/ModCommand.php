<?php

namespace OpenData\Irc\Command;

use Symfony\Component\Console\Command\Command;

class ModCommand extends Command {

    public function userHasSomePermissions($output) {
        
        return $this->isUserVoiced($output) ||
               $this->isUserAdmin($output);
    }
    
    public function isUserVoiced($output) {
        $user = $output->getUser();
        return in_array($user, $output->getBot()->getVoiced()) &&
               $this->getUsername($output, $user) != null;
    }
    
    public function getUsername($output, $nickname) {
        return $output->getBot()->getUsername($nickname);
    }
    
    public function isUserAdmin($output) {
        $user = $output->getUser();
        return in_array($user, $output->getBot()->getAdmins()) &&
               $this->getUsername($output, $user) != null;
    }
    
    public function userHasModPermissions($output, $mod) {
        
        if ($this->isUserAdmin($output)) {
            return true;
        }
        
        if (!$this->isUserVoiced($output)) {
            return false;
        }
        
        $nickname = $output->getUser();
        
        $username = $this->getUsername($output, $nickname);
        
        if ($username == null) return false;
        
        $admins = array();
        
        if (isset($mod['admins'])) {
            $admins = $mod['admins'];
        }
        
        return in_array($username, $admins);        
    }
}
