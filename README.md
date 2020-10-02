# Knowledge base URLs
## Deploying a docker
https://entermediadb.org/knowledge/10/docker-deployments/

## Installing Dev Environment
https://entermediadb.org/knowledge/10/ubuntu_installation_mint/
    Add project app-insights https://github.com/entermedia-community/app-insights or your own copy

# User/Manager URLs

## A. Self Create a user
    /insights/app/home/index.html?userid=<<user id>>

## B. emshare settings section
    /insights/emshare2/views/settings/events/triggers/edit/edit.html

## C. manager section
    /manager/views/sites/index.html

# Files Structure

## A. Groovy Script
    app-insights/webapp/WEB-INF/base/insights/catalog/events/scripts/data/ibmdiscoverysync.groovy

## B. Field Definition files
    app-insights/webapp/WEB-INF/base/insights/catalog/data/fields/insight_capability.xml
    app-insights/webapp/WEB-INF/base/insights/catalog/data/fields/insight_contract.xml
    app-insights/webapp/WEB-INF/base/insights/catalog/data/fields/insight_domain_poc.xml
    app-insights/webapp/WEB-INF/base/insights/catalog/data/fields/insight_platform.xml
    app-insights/webapp/WEB-INF/base/insights/catalog/data/fields/insight_product_mpl.xml
    app-insights/webapp/WEB-INF/base/insights/catalog/data/fields/insight_project_mip.xml
    app-insights/webapp/WEB-INF/base/insights/catalog/data/fields/insight_project_mvc.xml

## C. HTML files
    1. modal views
        app-insights/webapp/WEB-INF/base/insights/app/components/gridsample/preview/insight_capability.html
        app-insights/webapp/WEB-INF/base/insights/app/components/gridsample/preview/insight_contact.html
        app-insights/webapp/WEB-INF/base/insights/app/components/gridsample/preview/insight_domain_poc.html
        app-insights/webapp/WEB-INF/base/insights/app/components/gridsample/preview/insight_platform.html
        app-insights/webapp/WEB-INF/base/insights/app/components/gridsample/preview/insight_product_mpl.html
        app-insights/webapp/WEB-INF/base/insights/app/components/gridsample/preview/insight_project_mip.html
        app-insights/webapp/WEB-INF/base/insights/app/components/gridsample/preview/insight_project_mvc.html
    2. Cards
        app-insights.webapp.WEB-INF/base/insights/app/components/gridsample/types/discovery.html
    3. General tabs. summary, snippet, organization, people, jobs
        app-insights/webapp/WEB-INF/base/insights/app/components/gridsample/preview/tabs.html
    4. tags (keywords)
        app-insights/webapp/WEB-INF/base/insights/app/components/gridsample/preview/tags.html
    5. home site
        app-insights/webapp/WEB-INF/base/insights/app/home/index.html
    
## D. Source types definitions.
    app-insights/webapp/WEB-INF/base/catalog/data/lists/module/custom.xml
        You can add and remove source types here.

# Common actions

## common fields that should be modified only groovy script
    title
    text

## I Adding new fields
    1. Look for source type, "Field Definition"(B) file that matches the new field
    2. Add the a new field, (check other similar fields to see type)
    3. Make sure field doesn't exist already
    4. id=... should be ibmfield_name
    5. [Field Name] ...> Set the name as you like

## II Reindex
    1. Go to EMShare server settings http://<insightUrl>/insights/emshare2/views/settings/status/tools/index.html
    2. Clear Cached Data
    3. Reindex
    4. go to Scheduler http://<insightUrl>/insights/emshare2/views/settings/events/ps/index.html
    5. IBM Discovery Sync
    6. Run Now

## III Adding new fields NOT "title" or "text"
    1. Add Definition (I)
    2. Reindex (II)

## IV Adding "title" or "text" fields
    1. Add Definition (I)
    2. Go to Groovy Script File (A)
    3. Look for function "findRealField()"
    4. Look for source_type
    5. change return string with the preferred field
    6. Reindex (II)

# Building
## Add or change field(s) to modal a view
    1. Add field (IV or III) (title|text IV)
    2. Go to HTML source_type (C.1)
    3. $data willcontain all the fields defined in that source type (B)
        * e.g. $data.ibmtext will contain text
    4. Add new field where you want it

## Add or change field(s) to source type cards
    1. Add field (IV or III) (title|text IV)
    2. Go to HTML file (C.2)
    3. $hit willcontain all the fields defined in that source type (B)
        * e.g. $data.ibmtitle will contain title
    4. Add new field where you want it

## Special cases on title fields
    1. Add Definition (I)
    2. Go to Groovy Script File (A)
    3. Look for function specialCases()
        On this function, we defined the value of the field.
        e.g. MIP-Projects -> Title=chargeCode: longName
        We add those value fields to title


# Server Side Actions
    Building and deploying server

## Creating zip file
    1. Check out https://github.com/entermedia-community/app-insights (or your own copy)
    2. Check file app-insights/build.xml
    3. Make sure you have "ant" installed
    4. Run commands to build zip:
        $ git clone https://github.com/entermedia-community/app-insights
        $ cd app-insights
        $ ant
    5. You will get a message indicating location of your zip file: [zip] Building zip: ./deploy/builds/app-insights.zip

## Deploying zip file to entermedia
    Reference: $SITE and $NODE value's depends on how you created the docker, more details at: https://entermediadb.org/knowledge/10/docker-deployments/
    1. Go to server containing docker with entermedia
    2. Download file
        $ curl -s -o /media/emsites/$SITE/services/extensions/app-insights.zip http://your zip url/app-insights.zip
    3. Building app insights, and restarting container
        $ /media/emsites/$SITE/$NODE/update-em10dev.sh

## IBM Configuration values
    1. Go to EMShare2's Preferences Settings http://<URL>/insights/emshare2/views/settings/applicationsetup/theme/index.html
    2. Go to Tab "Catalog Parameters"
    3. You'll find Discovery fields:
        Discovery Search Start Year     # Defines the year that will start searching in discovery
        Discovery Search End Year       # Defines the year that will end searching in discovery
        Discovery Secret Key            # APIKEY
        Discovery URL                   # URL 
        Discovery Instance              # Instance ID
        Discovery ENV ID                # Environment ID
        Discovery Collection ID         # Collection ID

