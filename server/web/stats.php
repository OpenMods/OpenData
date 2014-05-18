<?php

require_once __DIR__ . '/../vendor/autoload.php';

$app = new Silex\Application();

require __DIR__ . '/../resources/config/prod.php';

require __DIR__ . '/../src/app.php';

$mongo = $app['mongo'];
$analytics = $mongo['analytics'];
$analytics->connect();
$db = $analytics->analytics;

$master = array();


$defaultClient = $mongo['default'];
$defaultDb = $defaultClient->hopper;

$mod = isset($_REQUEST['mod']) ? $_REQUEST['mod'] : 'openeye';


$signatures = array();
foreach ($defaultDb->files->find(
    array('mods.modId' => $mod),
    array('_id' => 1)
) as $file) {
    $signatures[] = $file['_id'];
}

if (count($signatures) == 0) {
	exit;
}
?>
<h2><?php echo $mod; ?></h2>
<?php

$timer = strtotime(date('Y-m-d H:00:00')) - (3600 * 2);

foreach ($db->analytics_signatures->find(array(
     '_id.type'     => 'signature',
     '_id.key'      => array('$in' => $signatures),
     '_id.span'     => 'hourly',
     '_id.time'		=> new MongoDate($timer)
)) as $hour) {
    foreach ($hour['value'] as $set) {
        if (!isset($master[$set['type']])) {
            $master[$set['type']] = array();
        }
        if (!isset($master[$set['type']][$set['key']])) {
            $master[$set['type']][$set['key']] = 0;
        }
        $master[$set['type']][$set['key']] += $set['value'];
    }
}

foreach ($master as $k => $v) {
    arsort($master[$k]);
}

echo "<pre>";
echo json_encode($master, JSON_PRETTY_PRINT);