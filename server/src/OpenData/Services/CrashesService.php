<?php

namespace OpenData\Services;

class CrashesService extends BaseService {

    public static function sanitizeGeneratedMethodNames($klazz) {

        $klazz = preg_replace('@GeneratedMethodAccessor[0-9]+@', 'GeneratedMethodAccessor', $klazz);
        $klazz = preg_replace('@ASMEventHandler_[0-9]+_@', 'ASMEventHandler_0_', $klazz);

        return $klazz;
    }

    public static function sanitizeMessage($arr) {

        if (isset($arr['message'])) {
            $arr['message'] = preg_replace("|([a-zA-Z0-9])@[a-f0-9]{1,8}(?![a-f0-9])|U", "$1[@ffffff]", $arr['message']);
        }

        if (isset($arr['exception']) && $arr['exception'] == 'java.net.UnknownHostException') {
            $arr['message'] = '';
        }

        return $arr['message'];
    }

    public static function stripSignatures($arr) {

        $signatures = array();
        $classes = array();

        for ($i = 0; $i < count($arr['stack']); $i++) {

            if (isset($arr['stack'][$i]['signatures'])) {
                $signatures = array_merge($arr['stack'][$i]['signatures'], $signatures);
            }

            $arr['stack'][$i]['class'] = self::sanitizeGeneratedMethodNames($arr['stack'][$i]['class']);
            $classes[] = $arr['stack'][$i]['class'];

            unset($arr['stack'][$i]['signatures']);
        }

        if (isset($arr['cause'])) {
            $cause = self::stripSignatures($arr['cause']);
            $signatures = array_merge($signatures, $cause['signatures']);
            $classes = array_merge($classes, $cause['classes']);
            $arr['cause'] = $cause['exception'];
        }

        $arr['message'] = self::sanitizeMessage($arr);

        return array(
            'signatures' => $signatures,
            'classes' => $classes,
            'exception' => $arr
        );
    }

    public static function getExceptionMessages($arr) {
        $messages[] = $arr['exception'] . ' ' . $arr['message'];
        while (isset($arr['cause'])) {
            $arr = $arr['cause'];
            $messages[] = $arr['exception'] . ': ' . $arr['message'];
        }
        return $messages;
    }

    public static function skipOpenEyeVersion($signatures) {
        return in_array('sha256:7a0786ce8c4666a685e92c9418d682ebd934274e5d150267067410639fb5a928', $signatures);
    }

    public function getCommonCrashDetails($packet) {

        if (isset($packet['exception'])) {

            $crashMessages = self::getExceptionMessages($packet['exception']);

            foreach ($this->db->common_crashes->find() as $commonCrash) {
                foreach ($commonCrash['regex'] as $regex) {
                    foreach ($crashMessages as $msg) {
                        if (preg_match("@" . $regex . "@i", $msg)) {
                            return $commonCrash;
                        }
                    }
                }
            }

        }
    }

    public function getCommonCrashBySlug($slug) {
        return $this->db->common_crashes->findOne(array(
                    'url' => $slug
        ));
    }

    public function add($packet, $shouldHide = false) {

        if (!isset($packet['exception'])) {
            return;
        }

        $note = null;

        $involvedSignatures = array();
        $involvedModIds = array();

        $allSignatures = array();
        $allModIds = array();

        // get all the modids and states from the mod states
        if (isset($packet['states'])) {
            foreach ($packet['states'] as $state) {
                if (isset($state['signature'])) {
                    $errored = false;
                    foreach ($state['mods'] as $mod) {
                        $sanitized = ModsService::sanitizeModId($mod['modId']);
                        if ($mod['state'] == 'Errored') {
                            $errored = true;
                            $involvedModIds[] = $sanitized;
                        }
                        $allModIds[] = $sanitized;
                    }
                    if ($errored) {
                        $involvedSignatures[] = $state['signature'];
                    }
                    $allSignatures[] = $state['signature'];
                }
            }
        }

        $crashData = self::stripSignatures($packet['exception']);
        $stackSignatures = $crashData['signatures'];
        $stackWithoutSignatures = $crashData['exception'];

        // if we've got stack signatures, lets get the modids for those
        // signatures
        if (count($stackSignatures) > 0) {
            $results = $this->db->files->find(
                    array('_id' => array('$in' => $stackSignatures)), array('mods.modId' => 1)
            );
            foreach ($results as $result) {
                if (isset($result['mods'])) {
                    foreach ($result['mods'] as $mod) {
                        $involvedModIds[] = $mod['modId'];
                    }
                }
            }
            // merge the stack signatures in
            $involvedSignatures = array_merge($involvedSignatures, $stackSignatures);
        }

        // get unique lists of both
        $involvedSignatures = array_unique($involvedSignatures);
        $involvedModIds = array_unique($involvedModIds);

        // find the hash of the stacktrace
        $packet['stackhash'] = md5(serialize($stackWithoutSignatures));

        // look for the stacktrace
        $crash = $this->db->crashes->findOne(array(
            '_id' => $packet['stackhash']
        ));

        $summarize = array();
        foreach (array('tags', 'javaVersion', 'side', 'minecraft', 'branding', 'location') as $key) {
            $summarize[$key] = array();
            if (isset($packet[$key])) {
                if (is_array($packet[$key])) {
                    $summarize[$key] = $packet[$key];
                } else {
                    $summarize[$key][] = $packet[$key];
                }
            }
        }

        foreach (array('mcp', 'fml', 'forge') as $key) {
            $summarize[$key] = array();
            if (isset($packet['runtime'][$key]) && is_string($packet['runtime'][$key])) {
                $summarize[$key][] = $packet['runtime'][$key];
            }
        }

        if (self::skipOpenEyeVersion($allSignatures)) {
            return null;
        }

        if ($crash == null) {
            $this->db->crashes->insert(array(
                '_id' => $packet['stackhash'],
                'latest' => time(),
                'exception' => $stackWithoutSignatures,
                'involvedSignatures' => array_values($involvedSignatures),
                'involvedMods' => array_values($involvedModIds),
                'allSignatures' => array_values($allSignatures),
                'allMods' => array_values($allModIds),
                'classes' => $crashData['classes'],
                'count' => 1,
                'tags' => $summarize['tags'],
                'javaVersions' => $summarize['javaVersion'],
                'minecraft' => $summarize['minecraft'],
                'branding' => $summarize['branding'],
                'side' => $summarize['side'],
                'location' => $summarize['location'],
                'mcp' => $summarize['mcp'],
                'fml' => $summarize['fml'],
                'forge' => $summarize['forge'],
                'resolved' => isset($packet['resolved']) ? $packet['resolved'] : true,
                'obfuscated' => isset($packet['obfuscated']) ? $packet['obfuscated'] : true,
                'hidden' => $shouldHide
            ));

            if (!$shouldHide && class_exists('\\Predis\\Client')) {
                $redis = new \Predis\Client();
                $redis->publish('crash', json_encode(array(
                    'modIds' => $involvedModIds,
                    'content' => 'New crash! ' . self::reduceMessageForIRC($stackWithoutSignatures['exception'] . ': ' . $stackWithoutSignatures['message']) . ' - http://openeye.openmods.info/crashes/' . $packet['stackhash']
                )));
            }
        } else {

            $crash['allSignatures'] = array_intersect($crash['allSignatures'], $allSignatures);
            $crash['allMods'] = array_intersect($crash['allMods'], $allModIds);

            if (isset($crash['note']) && isset($crash['note']['message'])) {
                $note = $crash['note'];
            }

            $this->db->crashes->update(
                    array('_id' => $packet['stackhash']), array(
                '$set' => array(
                    'latest' => time(),
                    'allSignatures' => array_values($crash['allSignatures']),
                    'allMods' => array_values($crash['allMods'])
                ),
                '$inc' => array('count' => 1),
                '$addToSet' => array(
                    'involvedSignatures' => array('$each' => $involvedSignatures),
                    'involvedMods' => array('$each' => $involvedModIds),
                    'tags' => array('$each' => $summarize['tags']),
                    'javaVersions' => array('$each' => $summarize['javaVersion']),
                    'side' => array('$each' => $summarize['side']),
                    'minecraft' => array('$each' => $summarize['minecraft']),
                    'branding' => array('$each' => $summarize['branding']),
                    'location' => array('$each' => $summarize['location']),
                    'fml' => array('$each' => $summarize['fml']),
                    'mcp' => array('$each' => $summarize['mcp']),
                    'forge' => array('$each' => $summarize['forge'])
                )
                    )
            );

            if ($crash['count'] == 10 || $crash['count'] == 100 || $crash['count'] == 500 || $crash['count'] == 1000) {
                $redis = new \Predis\Client();
                $redis->publish('crash', json_encode(array(
                    'modIds' => $involvedModIds,
                    'content' => $crash['count'] . ' crashes: ' . self::reduceMessageForIRC($stackWithoutSignatures['exception'] . ': ' . $stackWithoutSignatures['message']) . ' - http://openeye.openmods.info/crashes/' . $packet['stackhash']
                )));
            }
        }

        return array(
            'stackhash' => $packet['stackhash'],
            'note' => $note
        );
    }

    public static function reduceMessageForIRC($message, $length = 100, $dots = '...') {
        $message = trim(preg_replace('/\s+/', ' ', $message));
        $message = str_replace("\0", "[null]", $message);
        $message = (strlen($message) > $length) ? substr($message, 0, $length - strlen($dots)) . $dots : $message;
        return $message;
    }

    public function findByPackage($package, $skip = 0, $limit = 40) {
        return $this->find(array('classes' => new \MongoRegex('/^' . preg_quote($package) . '\./')))
                        ->sort(array('timestamp' => -1))
                        ->skip($skip)
                        ->limit($limit);
    }

    public function findBySignatures($signatures = array()) {
        if (count($signatures) == 0)
            return array();
        return $this->db->crashes->find(array('involvedSignatures' =>
                    array('$in' => $signatures)
                ))->sort(array('latest' => -1));
    }

    public function findByStackhash($stackhash) {
        return $this->db->crashes->findOne(array('_id' => $stackhash));
    }

    public function findLatest($query = array(), $skip = 0, $limit = 40) {
        return $this->find($query, array('resolved' => 1, 'tags' => 1, 'involvedMods' => 1, 'latest' => 1, 'exception' => 1))
                        ->sort(array('latest' => -1))
                        ->skip($skip)
                        ->limit($limit);
    }

    private function find($query = array()) {
        return $this->db->crashes->find($query);
    }

}
