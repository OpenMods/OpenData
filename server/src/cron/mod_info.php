<?php

require_once __DIR__ . '/../../vendor/autoload.php';

use JsonSchema\Uri\UriRetriever;
use JsonSchema\Validator;

$rustart = getrusage();
$time_start = microtime(true); 

$mongo = new MongoClient();

$db = $mongo->hopper;

$retriever = new UriRetriever();
$schema = $retriever->retrieve('file://' . __DIR__ . '/mod_info.json');

 
$copyKeys = array(
    'authors',
    'description',
    'credits',
    'url',
    'updateUrl',
    'tags',
    'videos'
);

foreach ($db->mods->find(array('jsonUrl' => array('$exists' => true))) as $mod) {
    
    $contents = file_get_contents($mod['jsonUrl']);
   
    
    try {
        $data = json_decode(mb_convert_encoding($contents, 'UTF-8', 'auto'));
        if ($data != null) {

            $validator = new Validator();
            $validator->check($packet, $data);
            if ($validator->isValid()) {
                
                $update = array();
                
                foreach ($copyKeys as $key) {
                    if ($data->$key != null) {
                        $update[$key] = $data->$key;
                    }
                }
                
                $changes = array();
                
                if (isset($data->image) && $data->image != '') {
                    $imgPath = __DIR__.'/../../web/modimages/'.$mod['_id'].'.png';
                    file_put_contents($imgPath, file_get_contents($data->image));
                    list($width, $height, $type, $attr)  = getimagesize($imgPath);
                    if (isset($type) && $type == IMAGETYPE_PNG && $width = 64 && $height == 64) {
                        $update['image'] = 'modimages/'.$mod['_id'].'.png';
                    } else {
                        $changes['$unset'] = array('image' => 1);
                        unlink($imgPath);
                    }
                }
                
                if (count($update) > 0) {
                    $changes['$set'] = $update;
                }
                if (count($changes) > 0) {
                    $db->mods->update(
                            array('_id' => $mod['_id']),
                            $changes                           
                    );
                }
                
            } else {
                echo "Invalid data file for mod ID ".$mod['_id']."\n";
            }

        } else {
            echo "Invalid data file for mod ID ".$mod['_id']."\n";
        }
        
    }catch (Exception $e) {
        print_r($e);
    }
}




function rutime($ru, $rus, $index) {
    return ($ru["ru_$index.tv_sec"]*1000 + intval($ru["ru_$index.tv_usec"]/1000))
     -  ($rus["ru_$index.tv_sec"]*1000 + intval($rus["ru_$index.tv_usec"]/1000));
}

$ru = getrusage();
$time_end = microtime(true);
$timeTaken = $time_end - $time_start;

$date = date("Y-m-d H:i:s");
echo "[".$date."] Computations: " . rutime($ru, $rustart, "utime")."\n";
echo "[".$date."] System calls: " . rutime($ru, $rustart, "stime")."\n";
echo "[".$date."] Memory: " . memory_get_peak_usage()."\n";
echo "[".$date."] Clock time: " . $timeTaken."\n";
 

