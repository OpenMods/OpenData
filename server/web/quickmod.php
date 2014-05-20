<?php

Header("Content-Type: text/plain");

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

$modId = isset($_REQUEST['mod']) ? $_REQUEST['mod'] : 'openeye';

$mod = $defaultDb->mods->findOne(array('_id' => $modId));

if ($mod == null) {
	exit;
}

$files = $defaultDb->files->find(array(
	'mods.modId' => $modId,
	'minecraft' => array('$exists' => true)
));

if (count($files) == 0) {
	exit;
}

$signatures = array();
foreach ($files as $file) {
	$signatures[$file['_id']] = $file;
}

$urls = $defaultDb->urls->find(array('_id' => array('$in' => array_keys($signatures))));

if (count($urls) == 0) {
	exit;
}

$useId = isset($mod['originalModId']) ? $mod['originalModId'] : $mod['_id'];

$quickMod = array(
	'formatVersion'	=> 1,
	'uid' 			=> $mod['_id'],
	'repo' 			=> 'openeye',
	'modId' 		=> isset($mod['originalModId']) ? $mod['originalModId'] : $mod['_id'],
	'name' 			=> $mod['name'],
	'description' 	=> $mod['description'],
	'updateUrl' 	=> 'http://openeye.openmods.info'.$_SERVER['REQUEST_URI'],
	'tags' 			=> isset($mod['tags']) ? $mod['tags'] : array(),
	'categories' 	=> isset($mod['tags']) ? $mod['tags'] : array(),
	'references'	=> array(),
	'versions'		=> array()
);

$references = array();

foreach ($urls as $url) {

	$signature = $signatures[$url['_id']];

	foreach ($signature['mods'] as $modDefinition) {
		if ($modDefinition['modId'] == $mod['_id']) {
			break;
		}
	}

	$version = array(
		'name' => $modDefinition['version'],
		'url' => $url['url'],
		'downloadType' => $url['jarUrl'] == $url['url'] ? 'parallel' : 'sequential',
		'mcCompat' => array(str_replace('Minecraft ', '', $signature['minecraft']))
	);


	if (isset($modDefinition['requiredMods'])) {
		$version['references'] = array();
		foreach ($modDefinition['requiredMods'] as $requirement) {
			$sanitized = strtolower(preg_replace("@[^a-z0-9_ ]+@i", '', $requirement['label']));
			if ($requirement['version'] == 'any') {
				$requirement['version'] = '(,)';
			}
			if ($sanitized == 'forge') {
				$version['forgeCompat'] = $requirement['version'];
			} else {
				$version['references'][] = array(
					'uid' => $sanitized,
					'type' => 'depends',
					'version' => $requirement['version']
				);
				$quickMod['references'][$sanitized] = 'http://openeye.openmods.info/quickmod.php?mod='.$sanitized;
			}

		}
	}

	$quickMod['versions'][] = $version;
}

echo json_encode($quickMod, JSON_PRETTY_PRINT | JSON_UNESCAPED_SLASHES);