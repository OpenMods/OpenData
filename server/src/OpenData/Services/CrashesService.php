<?php

namespace OpenData\Services;

class CrashesService extends BaseService {

    public function add($packet, $fileIds, $modIds) {

        $packet['stackhash'] = md5(serialize($packet['stack']));

        $uniqueCrash = $this->db->unique_crashes->findOne(
                array('stackhash' => $packet['stackhash'])
        );

        if ($uniqueCrash != null) {

            $uniqueCrash['mods'] = array_intersect($uniqueCrash['mods'], $modIds);
            $uniqueCrash['files'] = array_intersect($uniqueCrash['files'], $fileIds);

            $this->db->unique_crashes->update(
                array('stackhash' => $packet['stackhash']),
                array('$set' => array(
                    'latest'    => time(),
                    'mods'      => array_values($uniqueCrash['mods']),
                    'files'     => array_values($uniqueCrash['files'])
                ),
                '$inc' => array('count' => 1)
            ));

        } else {
            $this->db->unique_crashes->insert(array(
                'exception' => $packet['exception'],
                'stackhash' => $packet['stackhash'],
                'message'   => $packet['message'],
                'stack'     => $packet['stack'],
                'latest'    => time(),
                'count'     => 1,
                'mods'      => $modIds,
                'files'     => $fileIds
            ));
        }

        $this->db->crashes->insert($packet);
    }


    public function findByPackage($package, $skip = 0, $limit = 40) {
        return $this->find(array('stack.class' => new \MongoRegex('/^'.preg_quote($package).'\./')))
                ->sort(array('timestamp' => -1))
                ->skip($skip)
                ->limit($limit);

    }

    public function findUniqueBySignatures($signatures = array()) {
        if (count($signatures) == 0) return array();
        return $this->db->unique_crashes->find(array('stack.signatures' =>
                array('$in' => $signatures)
            ))->sort(array('latest' => -1));
    }

    public function findLatest($skip = 0, $limit = 40) {
        return $this->find()
                ->sort(array('timestamp' => -1))
                ->skip($skip)
                ->limit($limit);
    }

    private function find($query = array()) {
        return $this->db->crashes->find($query);
    }

}
