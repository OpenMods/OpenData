<?php

$rustart = getrusage();

$mongo = new MongoClient();

$db = $mongo->hopper;

$currentHour = strtotime(date("Y-m-d H:00:00"));
$previousHour = $currentHour - 3600;

$results = $db->analytics->aggregate(array(
    array('$match' => array('created_at' => array('$gte' => $previousHour, '$lt' => $currentHour))),
    array('$unwind' => '$signatures'),
    array('$group' => array('_id' => '$signatures.signature', 'launches' => array('$sum' => 1)))
));


$fileLaunches = array();
$modLaunches = array();


foreach ($results['result'] as $result) {
    $fileLaunches[$result['_id']] = $result['launches'];
}

$files = $db->files->find(
    array('_id' => array('$in' => array_keys($fileLaunches))),
    array('hours' => 1, 'mods.modId' => 1)
);

foreach ($files as $file) {
    
    $fileId = $file['_id'];
    
    $launchesOfThisFile = $fileLaunches[$fileId];
    
    $hours = isset($file['hours']) ? $file['hours'] : array();

    if (count($hours) == 48) {
        array_shift($hours);
    }
    
    $hours[] = array(
        'time' => new MongoDate($currentHour),
        'launches' => $launchesOfThisFile
    );
    
    $db->files->update(
        array('_id' => $fileId),
        array('$set' => array(
            'hours' => $hours            
        ))
    );
    
    foreach ($file['mods'] as $mod) {
        $modId = $mod['modId'];
        if (!isset($modLaunches[$modId])) {
            $modLaunches[$modId] = 0;
        }
        $modLaunches[$modId] += $launchesOfThisFile;
    }
}

$modDocuments = $db->mods->find(
    array('_id' => array('$in' => array_keys($modLaunches))),
    array('hours' => 1)
);

foreach ($modDocuments as $mod) {
    $modId = $mod['_id'];
    
    $hours = isset($mod['hours']) ? $mod['hours'] : array();
    
    if (count($hours) == 48) {
        array_shift($hours);
    }
    
    if (isset($modLaunches[$modId])) {
        $hours[] = array(
            'time' => new MongoDate($currentHour),
            'launches' => $modLaunches[$modId]
        );

        $db->mods->update(
            array('_id' => $modId),
            array('$set' => array(
                'hours' => $hours            
            ))
        );
    }
}

function rutime($ru, $rus, $index) {
    return ($ru["ru_$index.tv_sec"]*1000 + intval($ru["ru_$index.tv_usec"]/1000))
     -  ($rus["ru_$index.tv_sec"]*1000 + intval($rus["ru_$index.tv_usec"]/1000));
}

$ru = getrusage();
$date = date("Y-m-d H:i:s");
echo "[".$date."] Computations: " . rutime($ru, $rustart, "utime")."\n";
echo "[".$date."] System calls: " . rutime($ru, $rustart, "stime")."\n";