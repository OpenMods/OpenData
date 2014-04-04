<?php

namespace OpenData\Services;

class CrashesService extends BaseService {

    private static $VALID_KEYS = array(
        'branding',
        'forge',
        'minecraft',
        'mods',
        'exception',
        'stacktrace'
    );

    public function getAll($skip = 0, $limit = 30) {

        $cursor = $this->db->crashes->find()->skip($skip)->limit($limit);
        $crashes = array();
        foreach ($cursor as $crash) {
            $crashes[] = array(
                'id' => (string) $crash['_id'],
                'branding' => $crash['branding'],
                'forge' => $crash['forge'],
                'minecraft' => $crash['minecraft'],
                'mods' => $crash['mods'],
                'exception' => $crash['exception'],
                'stacktrace' => $crash['stacktrace'],
                'hash' => $crash['hash']
            );
        }
        return $crashes;
    }

    public function save($crash) {

        $crash = $this->verifyAndFormatCrash($crash);

        $this->db->crashes->insert($crash);

        return (string) $crash['_id'];
    }

    private function verifyAndFormatCrash($crash) {

        foreach (self::$VALID_KEYS as $key) {
            if (!isset($crash[$key])) {
                throw new \Exception('Invalid crash report. Expected key ' . $key);
            }
        }

        if (!is_array($crash['stacktrace'])) {
            throw new \Exception('Expected stacktrace to be an array');
        }

        foreach ($crash['stacktrace'] as $stack) {
            if (!is_string($stack)) {
                throw new \Exception('Expected stacktrace to be an array of strings');
            }
        }

        if (!is_string($crash['exception'])) {
            throw new \Exception('Expected exception to be a string');
        }

        if (!is_string($crash['forge'])) {
            throw new \Exception('Expected forge to be a string');
        }

        if (!is_string($crash['minecraft'])) {
            throw new \Exception('Expected minecraft to be a string');
        }

        if (!is_string($crash['branding'])) {
            throw new \Exception('Expected branding to be a string');
        }

        if (!is_array($crash['mods'])) {
            throw new \Exception('Expected mods to be an array');
        }

        foreach ($crash['mods'] as $mod) {
            if (!is_array($mod)) {
                throw new \Exception('Expected each mod to be an array');
            }
            if (count($mod) !== 2) {
                throw new \Exception('Unexpected data passed in mod');
            }
            if (!isset($mod['modId']) || !is_string($mod['modId'])) {
                throw new \Exception('Expected modId to be a string');
            }
            if (!isset($mod['version']) || !is_string($mod['version'])) {
                throw new \Exception('Expected version to be a string');
            }
        }

        foreach ($crash as $key => $value) {
            if (!in_array($key, self::$VALID_KEYS)) {
                throw new \Exception('Invalid key found ' . $key);
            }
        }

        // make a hash of the stacktrace for fast matching
        $crash['hash'] = md5(serialize($crash['stacktrace']));

        return $crash;
    }

}
