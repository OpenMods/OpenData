<?php

libxml_use_internal_errors(true);

$db = new MongoClient();
$collection = $db->hopper->mods;

// find a mod with a releases page that's never been checked before
$modDocument = $collection->findOne(
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
    $modDocuments = $collection->find(
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

$checkedUrls = isset($modDocument['checkedUrls']) ? $modDocument['checkedUrls'] : array();

$parsed = parse_url($updateUrl);

$newlyChecked = array();
$modUrls = array();

// differnet behaviour for curse

try {
    if ($parsed['host'] == 'minecraft.curseforge.com') {

        // grab all the 'file page' links from curse forge
        $dom = new DOMDocument('1.0');
        $html = file_get_contents($updateUrl);
        $dom->loadHTML($html);
        $finder = new DomXPath($dom);
        $nodes = $finder->query("//tbody/tr/td[contains(@class, 'col-file')]/a");
        for ($i = 0; $i < $nodes->length; $i++) {
            $node = $nodes->item($i);
            if ($node->attributes->length > 0) {
                $href = $node->attributes->item(0)->nodeValue;
                if (preg_match("|\/files\/|U", $href)) {
                    $fullUrl = 'http://minecraft.curseforge.com' . $href;
                    if (!in_array($fullUrl, $checkedUrls)) {
                        $newlyChecked[] = 'http://minecraft.curseforge.com' . $href;
                    }
                }
            }
        }

        $files = array();
        foreach ($newlyChecked as $page) {
            $dom = new DOMDocument('1.0');
            $html = file_get_contents($page);
            $dom->loadHTML($html);
            $finder = new DomXPath($dom);
            $nodes = $finder->query("//*[contains(@class, 'user-action')]//span/a");
            for ($i = 0; $i < $nodes->length; $i++) {
                $node = $nodes->item($i);
                $href = $node->attributes->item(0)->nodeValue;
                $files[] = $href;
            }
        }

        foreach ($files as $file) {
            $signature = 'sha256:' . hash('sha256', file_get_contents($file));
            $modUrls[$signature] = array(
                'jarUrl' => $file,
                'url' => $file
            );
        }
    } else {

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
                    $absolute = relativeToAbsolute($attr->nodeValue, $updateUrl);
                    if (in_array($absolute, $checkedUrls)) {
                        continue;
                    }
                    echo $absolute."\n";
                    $newlyChecked[] = $absolute;
                }
            }
            $files = array();
            foreach ($newlyChecked as $page) {
                $dom = new DOMDocument('1.0');
                $html = file_get_contents($page);
                $dom->loadHTML($html);
                $finder = new DomXPath($dom);
                $nodes = $finder->query("//table[contains(@class, 'fileList')]//a");
                for ($i = 0; $i < $nodes->length; $i++) {
                    $node = $nodes->item($i);
                    $href = $node->attributes->item(0)->nodeValue;
                    if (endsWith($href, '.jar') || endsWith($href, '.zip')) {
                        $absolute = relativeToAbsolute($href, $page);
                        echo $absolute."\n";
                        $files[] = $absolute;
                    }
                }
            }

            foreach ($files as $file) {
                $signature = 'sha256:' . hash('sha256', file_get_contents($file));
                $modUrls[$signature] = array(
                    'jarUrl' => $file,
                    'url' => $file
                );
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
                        $matches[] = trim($attribute->nodeValue);
                    }
                }
            }

            foreach ($matches as $match) {

                // make absolute
                $match = relativeToAbsolute($match, $baseUrl == null ? $updateUrl : $baseUrl);

                if (in_array($match, $checkedUrls)) {
                    continue;
                }

                $newlyChecked[] = $match;

                // resolve the adfly link (if it is one!)
                $jarUrl = resolveAdfly($match);

                $isProbablyAMod = (
                        endsWith($jarUrl, '.jar') ||
                        endsWith($jarUrl, '.zip') ||
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
                        $dom = new DOMDocument('1.0');
                        $contents = file_get_contents($jarUrl);
                        $dom->loadHTML($contents);
                        $finder = new DomXPath($dom);
                        $nodes = $finder->query("//a[@id=\"default_content_download_button\"]");
                        if ($nodes->length > 0) {
                            $href = $nodes->item(0)->attributes->getNamedItem('href');
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

$checkedUrls = array_unique(array_merge($checkedUrls, $newlyChecked));

foreach ($allModFiles as $modToInsert) {
    $db->hopper->urls->update(
        array('_id' => $modToInsert['_id']),
        $modToInsert,
        array('upsert' => true)
    );
}

$collection->update(
    array('_id' => $modDocument['_id']),
    array('$set' => array(
        'checkedUrls' => array_values($checkedUrls),
        'lastChecked' => time()
    ))
);

libxml_clear_errors();

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
