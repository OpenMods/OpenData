<?php

require_once __DIR__ . '/../../vendor/autoload.php';

$app = new Silex\Application();

require __DIR__ . '/../../resources/config/prod.php';
require __DIR__ . '/../app.php';

$mongo = $app['mongo'];
$conn = $mongo['default'];
$db = $conn->hopper;

libxml_use_internal_errors(true);

// find a mod with a releases page that's never been checked before
$modDocument = $db->mods->findOne(
    array(
        'releasesPage' => array(
            '$ne' => '',
            '$exists' => true
        ),
        'lastChecked' => array(
            '$exists' => false
        )
    )
);

// cant find one, lets get the one checked longest ago
if ($modDocument == null) {
    $modDocuments = $db->mods->find(
        array(
            'releasesPage' => array(
                '$ne' => '',
                '$exists' => true
            )
        )
    )->sort(array(
        'lastChecked' => 1
    ))->limit(1);

    if (count($modDocuments) == 0) {
        exit;
    }

    $modDocument = $modDocuments->getNext();
}

if ($modDocument == null)
    exit;

$updateUrl = $modDocument['releasesPage'];

echo "Checking ".$updateUrl."\n";

$parsed = parse_url($updateUrl);

$newlyChecked = array();
$modUrls = array();

// differnet behaviour for curse


try {

        // get the page
        $contents = file_get_contents($updateUrl);

        $isJenkins = preg_match("@buildHistory@Ui", $contents);

        if ($isJenkins) {
            $dom = new DOMDocument('1.0');
            $dom->loadHTML($contents);
            $finder = new DomXPath($dom);
            $nodes = $finder->query("//a[contains(@class, 'model-link')]");
            for ($i = 0; $i < $nodes->length; $i++) {
                $attr = $nodes->item($i)->attributes->getNamedItem('href');
                if ($attr != null) {
                    if (strlen($attr->nodeValue) > 0) {
                        $absolute = relativeToAbsolute($attr->nodeValue, $updateUrl);
                        echo $absolute."\n";
                        $newlyChecked[] = $absolute;
                    }
                }
            }

            $newlyChecked = reduceURLList($db, $newlyChecked);

            $files = array();
            foreach ($newlyChecked as $page) {
                $dom = new DOMDocument('1.0');

                $ctx = stream_context_create(array('http'=>array('timeout' => 40)));
                $html = @file_get_contents($page, false, $ctx);
                if ($html != null) {
                    $dom->loadHTML($html);
                    $finder = new DomXPath($dom);
                    $nodes = $finder->query("//table[contains(@class, 'fileList')]//a");
                    for ($i = 0; $i < $nodes->length; $i++) {
                        $node = $nodes->item($i);
                        $href = $node->attributes->item(0)->nodeValue;
                        if (strlen($href) > 0) {
                            if ((endsWith($href, '.jar') || endsWith($href, '.zip')) &&
                                substr_count(strtolower($href), '-api') == 0 &&
                                substr_count(strtolower($href), '-deobf') == 0 &&
                                substr_count(strtolower($href), '-src') == 0
                                ) {
                                $absolute = relativeToAbsolute($href, $page);
                                echo $absolute."\n";
                                $files[] = $absolute;
                            }
                        }
                    }
                }
            }

            foreach ($files as $file) {
                $ctx = stream_context_create(array('http'=>array('timeout' => 120)));
                $contents = @file_get_contents($file, false, $ctx);
                if ($contents != null) {
                    $signature = 'sha256:' . hash('sha256', $contents);
                    $modUrls[$signature] = array(
                        'jarUrl' => $file,
                        'url' => $file
                    );
                }
            }
        } else {

            $dom = new DOMDocument('1.0');
            $dom->loadHTML($contents);
            $finder = new DomXPath($dom);
            $nodes = $finder->query("//a");

            $baseNodes = $finder->query("//base");

            $baseUrl = null;
            if ($baseNodes->length > 0) {
                $attr = $baseNodes->item(0)->attributes->getNamedItem('href');
                if ($attr != null) {
                    $baseUrl = $attr->nodeValue;
                }
            }

            $matches = array();

            for ($i = 0; $i < $nodes->length; $i++) {
                $node = $nodes->item($i);
                foreach ($node->attributes as $attribute) {
                    if (strtolower($attribute->nodeName) == "href") {
                        $href = trim($attribute->nodeValue);
                        if (strlen($href) > 0) {
                            $u = relativeToAbsolute($href, $baseUrl == null ? $updateUrl : $baseUrl);
                            $newlyChecked[] = $u;
                        }
                    }
                }
            }

            $newlyChecked = reduceURLList($db, $newlyChecked);

            foreach ($newlyChecked as $match) {

                // resolve the adfly link (if it is one!)
                $jarUrl = resolveAdfly($match);

                $isProbablyAMod =  substr_count(strtolower($jarUrl), '-api') == 0 &&
                        substr_count(strtolower($jarUrl), '-deobf') == 0 &&
                        substr_count(strtolower($jarUrl), '-src') == 0 && (
                        endsWith($jarUrl, '.jar') ||
                        endsWith($jarUrl, '.zip') ||
                        (strpos($jarUrl, '/mc-mods/') !== false && endsWith($jarUrl, '/download')) ||
                        strpos($jarUrl, 'file=') !== false ||
                        strpos($jarUrl, 'f=') !== false
                        ) && strpos($jarUrl, '/archive/') === false;

                echo "IsMod? ".$jarUrl.": ".($isProbablyAMod ? "Yes": "No")."\n";

                if ($isProbablyAMod) {

                    $ch = curl_init();
                    curl_setopt($ch, CURLOPT_URL, $jarUrl);
                    curl_setopt($ch, CURLOPT_RETURNTRANSFER, 1);
                    curl_setopt($ch, CURLOPT_FOLLOWLOCATION, 1);
                    curl_setopt($ch, CURLOPT_SSL_VERIFYHOST, 0);
                    curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, 0);

                    curl_setopt($ch, CURLOPT_HEADER, 1);
                    $response = curl_exec($ch);

                    $header_size = curl_getinfo($ch, CURLINFO_HEADER_SIZE);
                    $header = substr($response, 0, $header_size);
                    $body = substr($response, $header_size);

                    $validMod = false;

                    if (preg_match("@https?:\/\/www\.dropbox\.com@i", $jarUrl)) {
                        echo "Dropbox link..\n";
                        $dom = new DOMDocument('1.0');
                        $contents = file_get_contents($jarUrl);
                        $dom->loadHTML($contents);
                        $finder = new DomXPath($dom);
                        $nodes = $finder->query("//a[@id=\"default_content_download_button\"]");
                        if ($nodes->length > 0) {
                            $href = $nodes->item(0)->attributes->getNamedItem('href');
                            echo "Found button link..\n";
                            if ($href != null) {
                                $jarUrl = $href->nodeValue;
                                $ch = curl_init();
                                curl_setopt($ch, CURLOPT_URL, $jarUrl);
                                curl_setopt($ch, CURLOPT_RETURNTRANSFER, 1);
                                curl_setopt($ch, CURLOPT_FOLLOWLOCATION, 1);
                                curl_setopt($ch, CURLOPT_SSL_VERIFYHOST, 0);
                                curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, 0);

                                curl_setopt($ch, CURLOPT_HEADER, 1);
                                $response = curl_exec($ch);

                                $header_size = curl_getinfo($ch, CURLINFO_HEADER_SIZE);
                                $header = substr($response, 0, $header_size);
                                $body = substr($response, $header_size);

                            }
                        }
                    }

                    foreach (explode("\n", $header) as $line) {
                        $parts = explode(": ", $line);
                        if ($parts[0] == "Content-Type") {
                            echo $parts[1]."\n";
                            if (in_array(trim($parts[1]), array("application/octet-stream", "application/x-java-archive", "application/zip", "application/java-archive"))) {
                                $validMod = true;
                                break;
                            }
                        }
                    }

                    if ($validMod) {
                        $signature = 'sha256:' . hash('sha256', $body);
                        $modUrls[$signature] = array(
                            'jarUrl' => $jarUrl,
                            'url' => $match
                        );
                    }
                }
            }
        }
} catch (\Exception $e) {
    // boq would kill me
}

$allModFiles = array();
foreach ($modUrls as $k => $mod) {
    $allModFiles[] = array(
        '_id' => $k,
        'jarUrl' => $mod['jarUrl'],
        'url' => $mod['url']
    );
}

foreach ($allModFiles as $modToInsert) {
    
    if ($file = $db->files->findOne(array('_id' => $modToInsert['_id']))) {
        
        $db->files->update(
            array('_id' => $modToInsert['_id']),
            array(
                '$set' => array(
                    'downloadUrl' => $modToInsert['url'],
                    'jarUrl' => $modToInsert['jarUrl']
                )
            )
        );
        
    } else {
        echo "inserting ".$modToInsert['_id']."\n";
        $db->urls->update(
            array('_id' => $modToInsert['_id']),
            $modToInsert,
            array('upsert' => true)
        );
    }
}

$db->mods->update(
    array('_id' => $modDocument['_id']),
    array('$set' => array('lastChecked' => time()))
);

foreach ($newlyChecked as $url) {
    $db->checked_urls->update(
        array('_id' => $url),
        array('_id' => $url),
        array('upsert' => true)
    );
}


libxml_clear_errors();

function reduceURLList($db, $urls) {
    $stored = $db->checked_urls->find(
            array('_id' => array('$in' => $urls))
    );
    $new = array();
    foreach($urls as $url) {
        $found = false;
        foreach ($stored as $storedUrl) {
            if ($storedUrl['_id'] == $url) {
                $found = true;
                break;
            }
        }
        if (!$found) {
             $new[] = $url;
        }
    }
    return $new;
}

function relativeToAbsolute($rel, $base) {
    if (parse_url($rel, PHP_URL_SCHEME) != '')
        return $rel;
    if ($rel[0] == '#' || $rel[0] == '?')
        return $base . $rel;
    extract(parse_url($base));
    $path = preg_replace('#/[^/]*$#', '', $path);
    if ($rel[0] == '/')
        $path = '';
    $abs = "$host".(!empty($port) ? ':'.$port : '')."$path/$rel";
    $re = array('#(/\.?/)#', '#/(?!\.\.)[^/]+/\.\./#');
    for ($n = 1; $n > 0; $abs = preg_replace($re, '/', $abs, -1, $n)) {

    }
    return $scheme . '://' . $abs;
}

function resolveAdfly($url) {
    if (!preg_match("@https?:\/\/adf\.ly\/[a-z0-9]+@i", $url)) {
        return $url;
    }
    $contents = file_get_contents($url);
    preg_match("@var ysmm = '([^']+)'@", $contents, $match);

    $ysmm = $match[1];
    $a = $t = '';
    for ($i = 0; $i < strlen($ysmm); $i++) {
        if ($i % 2 == 0) {
            $a .= $ysmm[$i];
        } else {
            $t = $ysmm[$i] . $t;
        }
    }

    $url = base64_decode($a . $t);
    $url = str_replace(' ', '%20', filter_var(strstr($url, 'http'), FILTER_SANITIZE_URL));
    return $url;
}

function endsWith($haystack, $needle) {
    return $needle === "" || substr($haystack, -strlen($needle)) === $needle;
}
